document.getElementById('toggle-password').addEventListener('click', function() {
  const input = document.getElementById('login-password');
  const eye = document.getElementById('icon-eye');
  const eyeOff = document.getElementById('icon-eye-off');
  const show = input.type === 'password';
  input.type = show ? 'text' : 'password';
  eye.hidden = !show;
  eyeOff.hidden = show;
  this.setAttribute('aria-pressed', show ? 'true' : 'false');
  this.setAttribute('aria-label', show ? 'Hide password' : 'Show password');
});