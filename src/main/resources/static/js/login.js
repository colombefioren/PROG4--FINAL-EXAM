document.getElementById("toggle-password").addEventListener("click", function () {
  const input = document.getElementById("login-password");
  const eye = document.getElementById("icon-eye");
  const eyeOff = document.getElementById("icon-eye-off");

  const isPasswordHidden = input.type === "password";

  input.type = isPasswordHidden ? "text" : "password";

  eye.toggleAttribute("hidden", !isPasswordHidden);
  eyeOff.toggleAttribute("hidden", isPasswordHidden);

  this.setAttribute("aria-pressed", isPasswordHidden ? "true" : "false");
  this.setAttribute("aria-label", isPasswordHidden ? "Hide password" : "Show password");
});
