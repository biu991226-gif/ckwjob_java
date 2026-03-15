function escapeHtml(value) {
  if (value == null) {
    return "";
  }
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function setActiveTab(tab) {
  const loginTab = document.getElementById("tab-login");
  const registerTab = document.getElementById("tab-register");
  const loginForm = document.getElementById("login-form-wrap");
  const registerForm = document.getElementById("register-form-wrap");

  if (tab === "register") {
    loginTab.classList.remove("is-active");
    registerTab.classList.add("is-active");
    loginForm.style.display = "none";
    registerForm.style.display = "block";
  } else {
    registerTab.classList.remove("is-active");
    loginTab.classList.add("is-active");
    registerForm.style.display = "none";
    loginForm.style.display = "block";
  }
}

function showErrors(errors) {
  const box = document.getElementById("message-error");
  if (!errors || errors.length === 0) {
    box.style.display = "none";
    box.innerHTML = "";
    return;
  }
  box.innerHTML = errors.map((error) => `<div>${escapeHtml(error)}</div>`).join("");
  box.style.display = "block";
}

function showSuccess(message) {
  const box = document.getElementById("message-success");
  if (!message) {
    box.style.display = "none";
    box.textContent = "";
    return;
  }
  box.textContent = message;
  box.style.display = "block";
}

async function submitForm(form, mode) {
  showErrors([]);
  showSuccess("");

  const formData = new FormData(form);
  const params = new URLSearchParams();
  for (const [key, value] of formData.entries()) {
    params.append(key, value);
  }
  params.set("mode", mode);

  try {
    const response = await fetch("Login", {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8"
      },
      body: params.toString()
    });
    const data = await response.json();

    if (data.success) {
      showSuccess(data.message);
      if (data.redirectUrl) {
        window.setTimeout(() => {
          window.location.href = data.redirectUrl;
        }, 500);
      }
      return;
    }

    showErrors(data.errors || ["処理に失敗しました。"]);
  } catch (error) {
    showErrors(["通信に失敗しました。"]);
  }
}

async function loadLoginState() {
  try {
    const response = await fetch("Login");
    const data = await response.json();
    const left = document.getElementById("top-nav-left");
    const right = document.getElementById("top-nav-right");
    const contextPath = window.location.pathname.split("/").slice(0, 2).join("/");

    let leftHtml = `
      <a href="${contextPath}/">トップ</a>
      <a href="${contextPath}/jobs.html">求人一覧</a>
    `;

    if (data.isLoggedIn) {
      leftHtml += `<a href="${contextPath}/my.html">個人ページ</a>`;
      if (data.currentRole === "company") {
        leftHtml += `<a href="${contextPath}/manage_jobs.html">求人管理</a>`;
      }
      right.innerHTML = `
        <span class="nav-user-text">ログイン中: ${escapeHtml(data.currentName)}（${escapeHtml(data.currentRole)}）</span>
        <a href="${contextPath}/logout.html">logout</a>
      `;
    } else {
      leftHtml += `<a href="${contextPath}/login.html">ログイン / 会員登録</a>`;
      right.innerHTML = "";
    }

    left.innerHTML = leftHtml;
  } catch (error) {
  }
}

document.addEventListener("DOMContentLoaded", () => {
  loadLoginState();

  document.getElementById("tab-login").addEventListener("click", (event) => {
    event.preventDefault();
    setActiveTab("login");
  });

  document.getElementById("tab-register").addEventListener("click", (event) => {
    event.preventDefault();
    setActiveTab("register");
  });

  document.getElementById("login-form").addEventListener("submit", (event) => {
    event.preventDefault();
    submitForm(event.currentTarget, "login");
  });

  document.getElementById("register-form").addEventListener("submit", (event) => {
    event.preventDefault();
    submitForm(event.currentTarget, "register");
  });

  setActiveTab("login");
});
