"""Minimal OpenRouter client for the free-model delegation experiment.

Standard library only. An eligible model is a `:free` catalog id whose pricing is
zero. Each run sends exactly one chat request for one model: no retries, no
`models` fallback list, and no provider routing options.
"""

import json
import time
import urllib.error
import urllib.request

API_BASE = "https://openrouter.ai/api/v1"
MODELS_URL = f"{API_BASE}/models"
CHAT_URL = f"{API_BASE}/chat/completions"
FREE_SUFFIX = ":free"
TIMEOUT_SECONDS = 60
MAX_OUTPUT_TOKENS = 512
TITLE = "MoreMinecrafts free-model delegation (Phase 0)"

# Preference order only. Selection falls back to the sorted eligible list.
PREFERRED_MODELS = (
    "google/gemma-4-31b-it:free",
    "nvidia/nemotron-3-super-120b-a12b:free",
    "thinkingmachines/inkling:free",
)

HTTP_HINTS = {
    402: "payment required; paid path is disabled, do not add credits",
    429: "rate limited; not retried",
}


class DelegationError(Exception):
    """Failure tagged with a kind: http, transport, validation, cost_guard or no_eligible_model."""

    def __init__(self, kind, message, http_status=None):
        super().__init__(message)
        self.kind = kind
        self.http_status = http_status


def urllib_transport(method, url, headers, body, timeout):
    """Default transport. Error messages never include the request headers or body."""
    request = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        hint = HTTP_HINTS.get(error.code, "")
        message = f"HTTP {error.code} from OpenRouter" + (f" ({hint})" if hint else "")
        raise DelegationError("http", message, error.code) from None
    except (urllib.error.URLError, OSError) as error:
        raise DelegationError("transport", f"transport failure: {type(error).__name__}") from None
    except ValueError:
        raise DelegationError("validation", "response body was not JSON") from None


class OpenRouterClient:
    def __init__(self, api_key, transport=urllib_transport):
        self._api_key = api_key
        self._transport = transport

    def list_models(self):
        """Catalog read. This is not a model request and consumes no completion tokens."""
        payload = self._transport("GET", MODELS_URL, {"Accept": "application/json"}, None, TIMEOUT_SECONDS)
        models = payload.get("data") if isinstance(payload, dict) else None
        if not isinstance(models, list):
            raise DelegationError("validation", "model catalog has no data list")
        return models

    def chat_once(self, model, messages):
        """Single completion request. Returns (payload, latency_ms)."""
        body = json.dumps(
            {
                "model": model,
                "messages": messages,
                "max_tokens": MAX_OUTPUT_TOKENS,
                "temperature": 0,
                "stream": False,
            }
        ).encode("utf-8")
        headers = {
            "Authorization": f"Bearer {self._api_key}",
            "Content-Type": "application/json",
            "X-Title": TITLE,
        }
        started = time.monotonic()
        payload = self._transport("POST", CHAT_URL, headers, body, TIMEOUT_SECONDS)
        return payload, round((time.monotonic() - started) * 1000)


def _is_zero_price(value):
    try:
        return float(value) == 0.0
    except (TypeError, ValueError):
        return False


def is_free_model(model):
    pricing = model.get("pricing") or {}
    return (
        str(model.get("id", "")).endswith(FREE_SUFFIX)
        and "prompt" in pricing
        and "completion" in pricing
        and all(_is_zero_price(value) for value in pricing.values())
    )


def select_model(catalog):
    eligible = {model["id"] for model in catalog if is_free_model(model)}
    if not eligible:
        raise DelegationError("no_eligible_model", "catalog has no free models")
    for model_id in PREFERRED_MODELS:
        if model_id in eligible:
            return model_id
    return sorted(eligible)[0]


def parse_chat_response(payload, requested_model):
    if not isinstance(payload, dict):
        raise DelegationError("validation", "response is not a JSON object")
    choices = payload.get("choices")
    if not isinstance(choices, list) or not choices or not isinstance(choices[0], dict):
        raise DelegationError("validation", "response has no choices")
    content = (choices[0].get("message") or {}).get("content")
    if not isinstance(content, str) or not content.strip():
        raise DelegationError("validation", "response content is empty")

    served = str(payload.get("model") or requested_model)
    if served not in (requested_model, requested_model.removesuffix(FREE_SUFFIX)):
        raise DelegationError("cost_guard", f"served model {served} differs from requested {requested_model}")

    usage = payload.get("usage") or {}
    cost = usage.get("cost")
    if cost is not None and not _is_zero_price(cost):
        raise DelegationError("cost_guard", "response reports a non-zero cost")

    return {
        "model_served": served,
        "content": content,
        "finish_reason": choices[0].get("finish_reason"),
        "usage": {key: usage.get(key) for key in ("prompt_tokens", "completion_tokens", "total_tokens")},
        "cost_reported": cost,
    }
