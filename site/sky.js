/* Original procedural sky: no textures, dependencies or network requests. */
(() => {
  const canvas = document.querySelector('.sky-canvas');
  const gl = canvas.getContext('webgl', { alpha: false, antialias: false, depth: false, powerPreference: 'low-power' });
  if (!gl) return;
  const vertex = 'attribute vec2 position; void main(){gl_Position=vec4(position,0.0,1.0);}';
  const fragment = `
    precision highp float;
    uniform vec2 resolution;
    uniform float time;
    uniform vec2 lean;
    float hash(vec2 p) { return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453); }
    float noise(vec2 p) {
      vec2 i=floor(p), f=fract(p); f=f*f*(3.0-2.0*f);
      return mix(mix(hash(i),hash(i+vec2(1.,0.)),f.x),mix(hash(i+vec2(0.,1.)),hash(i+1.),f.x),f.y);
    }
    float cloud(vec2 p) {
      float n=0., a=.54;
      for(int i=0;i<6;i++) { n+=a*noise(p); p=mat2(1.6,1.2,-1.2,1.6)*p+3.7; a*=.48; }
      return n;
    }
    void main() {
      vec2 uv=gl_FragCoord.xy/resolution;
      float aspect=resolution.x/resolution.y;
      vec2 p=vec2(uv.x*aspect,uv.y)*3.5;
      p+=vec2(time*.009,time*.002)+lean*.055;
      vec2 warp=vec2(cloud(p*.7),cloud(p*.7+8.));
      float billow=cloud(p+warp*.85);
      float sides=pow(abs(uv.x-.5)*2.,1.3);
      float bank=(1.-smoothstep(.15,.85,uv.y))*.13+sides*.15;
      float mobile=1.-smoothstep(.65,1.15,aspect);
      float clearing=(1.-smoothstep(mix(.12,.23,mobile),mix(.43,.54,mobile),abs(uv.x-.5)))*smoothstep(.28,.50,uv.y);
      float density=smoothstep(.47-bank,.64-bank,billow);
      density*=1.-clearing*.99;
      float upper=cloud(p+warp*.85+vec2(-.065,.08));
      float light=clamp(.72+(billow-upper)*3.5,.42,1.);
      vec3 sky=mix(vec3(.45,.68,.85),vec3(.12,.36,.68),smoothstep(0.,1.,uv.y));
      sky+=vec3(.10,.09,.04)*pow(1.-uv.x,3.)*.6;
      vec3 shade=mix(vec3(.53,.66,.78),vec3(1.,.98,.94),light);
      vec3 color=mix(sky,shade,density*.97);
      color=mix(color,vec3(.76,.84,.89),pow(1.-uv.y,3.)*.2);
      gl_FragColor=vec4(color,1.);
    }`;
  function shader(type, source) {
    const result = gl.createShader(type);
    gl.shaderSource(result, source);
    gl.compileShader(result);
    if (!gl.getShaderParameter(result, gl.COMPILE_STATUS)) { gl.deleteShader(result); return null; }
    return result;
  }
  const vs = shader(gl.VERTEX_SHADER, vertex), fs = shader(gl.FRAGMENT_SHADER, fragment);
  if (!vs || !fs) return;
  const program = gl.createProgram();
  gl.attachShader(program, vs); gl.attachShader(program, fs); gl.linkProgram(program);
  gl.deleteShader(vs); gl.deleteShader(fs);
  if (!gl.getProgramParameter(program, gl.LINK_STATUS)) return;
  gl.useProgram(program);
  const buffer = gl.createBuffer(); gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
  gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1,-1,1,-1,-1,1,-1,1,1,-1,1,1]), gl.STATIC_DRAW);
  const position = gl.getAttribLocation(program, 'position');
  gl.enableVertexAttribArray(position); gl.vertexAttribPointer(position, 2, gl.FLOAT, false, 0, 0);
  const size = gl.getUniformLocation(program, 'resolution'), clock = gl.getUniformLocation(program, 'time');
  const tilt = gl.getUniformLocation(program, 'lean');
  const motion = matchMedia('(prefers-reduced-motion: reduce)');
  const fine = matchMedia('(hover: hover) and (pointer: fine)');
  let leanX = 0, leanY = 0, aimX = 0, aimY = 0;
  let visible = true, frame = 0, elapsed = 0, previous = 0, lost = false;
  function draw() {
    // Bound pixel work, independent of device pixel ratio. CSS scales the soft sky.
    const scale = Math.min(1, 1100 / canvas.clientWidth);
    const width = Math.max(1, Math.round(canvas.clientWidth * scale));
    const height = Math.max(1, Math.round(canvas.clientHeight * scale));
    if (canvas.width !== width || canvas.height !== height) { canvas.width=width; canvas.height=height; gl.viewport(0,0,width,height); }
    gl.uniform2f(size,width,height); gl.uniform1f(clock,elapsed);
    gl.uniform2f(tilt,leanX,leanY);
    gl.drawArrays(gl.TRIANGLES,0,6);
  }
  function tick(now) {
    frame = 0;
    if (!visible || document.hidden || motion.matches || lost) { previous=0; return; }
    if (!previous || now-previous >= 1000/30) {
      if (previous) elapsed += Math.min((now-previous)/1000,.1);
      previous=now; leanX+=(aimX-leanX)*.12; leanY+=(aimY-leanY)*.12; draw();
    }
    frame=requestAnimationFrame(tick);
  }
  function sync() {
    cancelAnimationFrame(frame); frame=0; previous=0;
    if (motion.matches || !fine.matches) { leanX=0; leanY=0; aimX=0; aimY=0; }
    if (!lost) draw();
    if (visible && !document.hidden && !motion.matches && !lost) frame=requestAnimationFrame(tick);
  }
  new IntersectionObserver(entries => { visible=entries[0].isIntersecting; sync(); }).observe(canvas);
  new ResizeObserver(sync).observe(canvas);
  motion.addEventListener('change',sync);
  fine.addEventListener('change',sync);
  window.addEventListener('pointermove', event => {
    if (!visible || motion.matches || !fine.matches) return;
    const bounds=canvas.getBoundingClientRect();
    aimX=Math.max(-1,Math.min(1,(event.clientX-bounds.left)/bounds.width*2-1));
    aimY=Math.max(-1,Math.min(1,(event.clientY-bounds.top)/bounds.height*2-1));
  }, { passive: true });
  document.documentElement.addEventListener('pointerleave', () => { aimX=0; aimY=0; });
  document.addEventListener('visibilitychange',sync);
  canvas.addEventListener('webglcontextlost',event => { event.preventDefault(); lost=true; cancelAnimationFrame(frame); canvas.classList.remove('is-ready'); });
  // Keep the CSS fallback after context loss; no repeated context allocation.
  draw(); canvas.classList.add('is-ready'); sync();
})();
