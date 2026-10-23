"""Mocked unit tests. No network access and no API key are needed.

Run from the repository root:
    python3 -m unittest discover -s scripts/free_model_delegation -p "test_*.py" -v
"""

import io
import json
import os
import tempfile
import unittest
import urllib.error
from unittest import mock

import openrouter_client as client_module
import run_delegation
from openrouter_client import (
    CHAT_URL,
    MODELS_URL,
    DelegationError,
    OpenRouterClient,
    is_free_model,
    parse_chat_response,
    select_model,
    urllib_transport,
)

FREE = {"id": "vendor/model-a:free", "pricing": {"prompt": "0", "completion": "0"}}
PAID = {"id": "vendor/model-b", "pricing": {"prompt": "0.000001", "completion": "0.000002"}}
ZERO_NO_SUFFIX = {"id": "vendor/model-c", "pricing": {"prompt": "0", "completion": "0"}}
NO_PRICING = {"id": "vendor/model-d:free"}
FAKE_KEY = "sk-or-test-SECRET-1234567890"


def completion(model="vendor/model-a:free", content="A short answer.", cost=None):
    usage = {"prompt_tokens": 5, "completion_tokens": 4, "total_tokens": 9}
    if cost is not None:
        usage["cost"] = cost
    return {
        "model": model,
        "choices": [{"message": {"role": "assistant", "content": content}, "finish_reason": "stop"}],
        "usage": usage,
    }


class FakeTransport:
    def __init__(self, models=None, chat=None, chat_error=None):
        self.models = [FREE] if models is None else models
        self.chat = completion() if chat is None else chat
        self.chat_error = chat_error
        self.calls = []

    def __call__(self, method, url, headers, body, timeout):
        self.calls.append({"method": method, "url": url, "headers": headers, "body": body})
        if url == MODELS_URL:
            return {"data": self.models}
        if self.chat_error:
            raise self.chat_error
        return self.chat


def refuse_any_call(method, url, headers, body, timeout):
    raise AssertionError("no network call was expected")


class FreeModelSelectionTests(unittest.TestCase):
    def test_accepts_zero_priced_free_suffix_model(self):
        self.assertTrue(is_free_model(FREE))

    def test_rejects_paid_model(self):
        self.assertFalse(is_free_model(PAID))

    def test_rejects_zero_priced_model_without_free_suffix(self):
        self.assertFalse(is_free_model(ZERO_NO_SUFFIX))

    def test_rejects_model_without_pricing(self):
        self.assertFalse(is_free_model(NO_PRICING))

    def test_select_prefers_known_model_order(self):
        preferred = client_module.PREFERRED_MODELS[1]
        catalog = [FREE, {"id": preferred, "pricing": {"prompt": "0", "completion": "0"}}]
        self.assertEqual(select_model(catalog), preferred)

    def test_select_falls_back_to_sorted_first_eligible(self):
        zed = {"id": "zed/model:free", "pricing": {"prompt": "0", "completion": "0"}}
        self.assertEqual(select_model([zed, FREE, PAID]), "vendor/model-a:free")

    def test_select_raises_when_no_free_model(self):
        with self.assertRaises(DelegationError) as raised:
            select_model([PAID, ZERO_NO_SUFFIX])
        self.assertEqual(raised.exception.kind, "no_eligible_model")


class ChatRequestTests(unittest.TestCase):
    def test_exactly_one_chat_request_with_single_model_and_no_fallback_fields(self):
        transport = FakeTransport()
        record = run_delegation.delegate(FAKE_KEY, transport, "live")

        self.assertEqual(record["status"], "ok")
        chat_calls = [call for call in transport.calls if call["url"] == CHAT_URL]
        self.assertEqual(len(chat_calls), 1)
        body = json.loads(chat_calls[0]["body"])
        self.assertEqual(body["model"], "vendor/model-a:free")
        self.assertEqual(body["max_tokens"], client_module.MAX_OUTPUT_TOKENS)
        self.assertFalse(body["stream"])
        for forbidden in ("models", "route", "provider", "fallbacks"):
            self.assertNotIn(forbidden, body)
        self.assertEqual(chat_calls[0]["headers"]["Authorization"], f"Bearer {FAKE_KEY}")

    def test_no_retry_after_chat_transport_failure(self):
        transport = FakeTransport(chat_error=DelegationError("transport", "transport failure: TimeoutError"))
        record = run_delegation.delegate(FAKE_KEY, transport, "live")

        self.assertEqual(record["status"], "error_transport")
        self.assertEqual(len([c for c in transport.calls if c["url"] == CHAT_URL]), 1)
        self.assertEqual(len(transport.calls), 2)  # one catalog read plus one chat request


class ResponseValidationTests(unittest.TestCase):
    def test_valid_response_is_parsed(self):
        parsed = parse_chat_response(completion(), "vendor/model-a:free")
        self.assertEqual(parsed["content"], "A short answer.")
        self.assertEqual(parsed["usage"]["total_tokens"], 9)
        self.assertEqual(parsed["model_served"], "vendor/model-a:free")

    def test_suffix_stripped_served_id_is_accepted(self):
        parsed = parse_chat_response(completion(model="vendor/model-a"), "vendor/model-a:free")
        self.assertEqual(parsed["model_served"], "vendor/model-a")

    def test_empty_content_is_validation_error(self):
        with self.assertRaises(DelegationError) as raised:
            parse_chat_response(completion(content="   "), "vendor/model-a:free")
        self.assertEqual(raised.exception.kind, "validation")

    def test_missing_choices_is_validation_error(self):
        with self.assertRaises(DelegationError) as raised:
            parse_chat_response({"model": "vendor/model-a:free"}, "vendor/model-a:free")
        self.assertEqual(raised.exception.kind, "validation")

    def test_different_served_model_is_cost_guard(self):
        with self.assertRaises(DelegationError) as raised:
            parse_chat_response(completion(model="vendor/paid-fallback"), "vendor/model-a:free")
        self.assertEqual(raised.exception.kind, "cost_guard")

    def test_nonzero_reported_cost_is_cost_guard(self):
        with self.assertRaises(DelegationError) as raised:
            parse_chat_response(completion(cost=0.0004), "vendor/model-a:free")
        self.assertEqual(raised.exception.kind, "cost_guard")

    def test_zero_reported_cost_is_allowed(self):
        parsed = parse_chat_response(completion(cost=0), "vendor/model-a:free")
        self.assertEqual(parsed["cost_reported"], 0)


class HttpErrorTests(unittest.TestCase):
    def test_http_error_is_redacted_and_keeps_status(self):
        body = io.BytesIO(f'{{"error": "bad key {FAKE_KEY}"}}'.encode())
        error = urllib.error.HTTPError(CHAT_URL, 402, "Payment Required", {}, body)
        request_path = "openrouter_client.urllib.request.urlopen"
        with mock.patch(request_path, side_effect=error):
            with self.assertRaises(DelegationError) as raised:
                urllib_transport("POST", CHAT_URL, {"Authorization": f"Bearer {FAKE_KEY}"}, b"{}", 5)

        self.assertEqual(raised.exception.kind, "http")
        self.assertEqual(raised.exception.http_status, 402)
        self.assertNotIn(FAKE_KEY, str(raised.exception))
        self.assertIn("paid path is disabled", str(raised.exception))


class RunDelegationTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.out = os.path.join(self.tmp.name, "out")

    def read_outputs(self):
        with open(os.path.join(self.out, "result.json"), encoding="utf-8") as handle:
            result = json.load(handle)
        with open(os.path.join(self.out, "summary.md"), encoding="utf-8") as handle:
            summary = handle.read()
        return result, summary

    def test_mock_mode_runs_offline_and_reports_mock_ok(self):
        with mock.patch.object(client_module.urllib.request, "urlopen", side_effect=AssertionError("network")):
            record = run_delegation.run("mock", self.out, {})
        self.assertEqual(record["status"], "mock_ok")
        self.assertEqual(record["usage"]["total_tokens"], 120)
        result, summary = self.read_outputs()
        self.assertEqual(result["status"], "mock_ok")
        self.assertIn("mock_ok", summary)

    def test_live_without_key_is_blocked_and_sends_nothing(self):
        record = run_delegation.run("live", self.out, {}, transport=refuse_any_call)
        self.assertEqual(record["status"], "blocked_no_key")
        result, summary = self.read_outputs()
        self.assertEqual(result["status"], "blocked_no_key")
        self.assertIn("blocked_no_key", summary)

    def test_live_success_records_identity_usage_and_latency_without_key(self):
        env = {"OPENROUTER_API_KEY": FAKE_KEY}
        transport = FakeTransport()
        record = run_delegation.run("live", self.out, env, transport=transport)

        self.assertEqual(record["status"], "ok")
        self.assertEqual(record["model_requested"], "vendor/model-a:free")
        self.assertEqual(record["model_served"], "vendor/model-a:free")
        self.assertIsInstance(record["latency_ms"], int)
        self.assertEqual(record["usage"]["prompt_tokens"], 5)
        result, summary = self.read_outputs()
        self.assertNotIn(FAKE_KEY, json.dumps(result))
        self.assertNotIn(FAKE_KEY, summary)

    def test_live_paid_fallback_is_failure_exit_code(self):
        transport = FakeTransport(chat=completion(model="vendor/paid-fallback", cost=0.01))
        record = run_delegation.run("live", self.out, {"OPENROUTER_API_KEY": FAKE_KEY}, transport=transport)
        self.assertEqual(record["status"], "error_cost_guard")
        self.assertIsNone(record["response"])

    def test_main_exit_codes(self):
        with mock.patch.dict(os.environ, {}, clear=True):
            self.assertEqual(run_delegation.main(["--mode", "mock", "--out", self.out]), 0)
            self.assertEqual(run_delegation.main(["--mode", "live", "--out", self.out]), 0)

    def test_step_summary_is_appended_when_present(self):
        summary_path = os.path.join(self.tmp.name, "step-summary.md")
        run_delegation.run("live", self.out, {"GITHUB_STEP_SUMMARY": summary_path})
        with open(summary_path, encoding="utf-8") as handle:
            self.assertIn("blocked_no_key", handle.read())


if __name__ == "__main__":
    unittest.main()
