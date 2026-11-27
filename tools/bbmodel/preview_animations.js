// Headless contact-sheet previewer for .bbmodel animations (editor convention:
// raw Euler ZYX, no Bedrock sign flips). Not a game-runtime check.
// Usage: node tools/bbmodel/preview_animations.js <model.bbmodel> <outdir> [animName ...]
// Needs: playwright (global) and `npm i three@0.160.0` resolvable from the cwd.
// Each PNG: top row 3/4 view, bottom row side view, 8 evenly spaced frames.
const {chromium}=require('playwright');
const fs=require('fs');
const [,, model, outdir, ...anims]=process.argv;
(async()=>{
const b=await chromium.launch(); const p=await b.newPage({viewport:{width:1600,height:700}});
const bb=JSON.parse(fs.readFileSync(model,'utf8'));
await p.setContent('<body style="margin:0;background:#20242c"><canvas id=c width=1600 height=700></canvas></body>');
await p.addScriptTag({content:fs.readFileSync((process.env.THREE_MIN||'node_modules/three/build/three.min.js'),'utf8')});
await p.evaluate((bb)=>{
const S=1/16;
const R=new THREE.WebGLRenderer({canvas:document.getElementById('c'),antialias:true,preserveDrawingBuffer:true});
R.setScissorTest(true);
const scene=new THREE.Scene(); scene.background=new THREE.Color(0x20242c);
scene.add(new THREE.AmbientLight(0xffffff,0.6)); const dl=new THREE.DirectionalLight(0xffffff,0.8); dl.position.set(-2,4,-3); scene.add(dl);
const grid=new THREE.GridHelper(4,16,0x445,0x334); scene.add(grid);
const els={}; bb.elements.forEach(e=>els[e.uuid]=e);
const grp={}; bb.groups.forEach(g=>grp[g.uuid]=g);
const bones={}; const root=new THREE.Group(); root.scale.setScalar(S); scene.add(root);
let hue=0;
function build(node,parent,po){
 if(typeof node==='string'){const e=els[node]; if(!e||e.type!=='cube')return;
  const f=e.from,t=e.to,s=[t[0]-f[0],t[1]-f[1],t[2]-f[2]];
  const m=new THREE.Mesh(new THREE.BoxGeometry(s[0],s[1],s[2]),parent.userData.mat);
  const c=[(f[0]+t[0])/2,(f[1]+t[1])/2,(f[2]+t[2])/2];
  if(e.rotation&&e.origin){const o=new THREE.Group();o.position.set(e.origin[0]-po[0],e.origin[1]-po[1],e.origin[2]-po[2]);o.rotation.order='ZYX';o.rotation.set(...e.rotation.map(d=>d*Math.PI/180));parent.add(o);m.position.set(c[0]-e.origin[0],c[1]-e.origin[1],c[2]-e.origin[2]);o.add(m);}
  else{m.position.set(c[0]-po[0],c[1]-po[1],c[2]-po[2]);parent.add(m);}
  return;}
 const g=grp[node.uuid]; const o=new THREE.Group(); o.rotation.order='ZYX';
 o.position.set(g.origin[0]-po[0],g.origin[1]-po[1],g.origin[2]-po[2]);
 const r=(g.rotation||[0,0,0]).map(d=>d*Math.PI/180); o.rotation.set(r[0],r[1],r[2]);
 o.userData.rest={p:o.position.clone(),r:o.rotation.clone()};
 o.userData.mat=new THREE.MeshLambertMaterial({color:new THREE.Color().setHSL((hue++*0.13)%1,0.45,0.55)});
 parent.add(o); bones[node.uuid]=o;
 (node.children||[]).forEach(c=>build(c,o,g.origin));
}
root.userData.mat=new THREE.MeshLambertMaterial({color:0x888888});
bb.outliner.forEach(n=>build(n,root,[0,0,0]));
function interp(kfs,t){ if(!kfs.length)return null; kfs=kfs.slice().sort((a,b)=>a.time-b.time);
 const v=k=>k.data_points[0]?['x','y','z'].map(a=>+k.data_points[0][a]):[0,0,0];
 if(t<=kfs[0].time)return v(kfs[0]); if(t>=kfs[kfs.length-1].time)return v(kfs[kfs.length-1]);
 for(let i=0;i<kfs.length-1;i++){const a=kfs[i],b=kfs[i+1]; if(t>=a.time&&t<=b.time){const u=(t-a.time)/(b.time-a.time);const A=v(a),B=v(b);return A.map((x,j)=>x+(B[j]-x)*u);}}}
window.pose=(name,t)=>{
 for(const k in bones){const o=bones[k];o.position.copy(o.userData.rest.p);o.rotation.copy(o.userData.rest.r);o.scale.set(1,1,1);}
 const an=bb.animations.find(a=>a.name.split('.').pop()===name); if(!an)return false;
 for(const id in an.animators){const o=bones[id]; if(!o)continue; const ks=an.animators[id].keyframes;
  const rot=interp(ks.filter(k=>k.channel==='rotation'),t),pos=interp(ks.filter(k=>k.channel==='position'),t),sc=interp(ks.filter(k=>k.channel==='scale'),t);
  if(rot){o.rotation.x+=rot[0]*Math.PI/180;o.rotation.y+=rot[1]*Math.PI/180;o.rotation.z+=rot[2]*Math.PI/180;}
  if(pos){o.position.x+=pos[0];o.position.y+=pos[1];o.position.z+=pos[2];}
  if(sc){o.scale.x*=sc[0];o.scale.y*=sc[1];o.scale.z*=sc[2];}}
 return true;};
window.animList=()=>bb.animations.map(a=>[a.name.split('.').pop(),a.length]);
window.shot=(name,n)=>{ window.pose('__rest',0); window.__rb=new THREE.Box3().setFromObject(root); const _s=window.__rb.getSize(new THREE.Vector3()); const _m=Math.max(_s.x,_s.y,_s.z); window.__rb.expandByScalar(_m*0.05);
 const an=bb.animations.find(a=>a.name.split('.').pop()===name); const L=an.length;
 R.setSize(1600,700); R.setScissor(0,0,1600,700); R.setClearColor(0x20242c); R.clear();
 const w=1600/n, h=350;
 for(let row=0;row<2;row++)for(let i=0;i<n;i++){
  window.pose(name,L*i/n);
  const cam=new THREE.PerspectiveCamera(35,w/h,0.1,50);
  const rb=window.__rb; 
  const c=new THREE.Vector3(); rb.getCenter(c); const sz=rb.getSize(new THREE.Vector3()); const d=Math.max(sz.x,sz.y,sz.z)*1.9;
  if(row===0)cam.position.set(c.x-d*0.7,c.y+d*0.35,c.z-d*0.7); else cam.position.set(c.x-d,c.y,c.z);
  cam.lookAt(c);
  R.setViewport(i*w,350*(1-row),w,h);R.setScissor(i*w,350*(1-row),w,h);R.render(scene,cam);}
};
},bb);
fs.mkdirSync(outdir,{recursive:true});
const list=await p.evaluate(()=>window.animList());
for(const [n] of list){ if(anims.length&&!anims.includes(n))continue;
 await p.evaluate(([n])=>window.shot(n,8),[n]);
 await p.screenshot({path:`${outdir}/${n}.png`}); console.log('shot',n);}
await b.close();})();
