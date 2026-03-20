function escapeHtml(value) {
  if (value == null) return "";
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function updateCompanyNav(data) {
  const left = document.getElementById("top-nav-left");
  const right = document.getElementById("top-nav-right");
  const contextPath = window.location.pathname.split("/").slice(0, 2).join("/");

  let leftHtml = `
    <a href="${contextPath}/">トップ</a>
    <a href="${contextPath}/jobs.html">求人一覧</a>
  `;

  if (!data.isLoggedIn) {
    leftHtml += `<a href="${contextPath}/login.html">ログイン / 会員登録</a>`;
    right.innerHTML = "";
  } else {
    leftHtml += `<a href="${contextPath}/my.html">個人ページ</a>`;
    if (data.currentRole === "company") {
      leftHtml += `<a href="${contextPath}/manage_jobs.html">求人管理</a>`;
      leftHtml += `<a href="${contextPath}/add_job.html">求人登録</a>`;
    }
    right.innerHTML = `
      <span class="nav-user-text">ログイン中: ${escapeHtml(data.currentName)}</span>
      <a href="${contextPath}/logout.html">ログアウト</a>
    `;
  }

  left.innerHTML = leftHtml;
}

async function loadState() {
  const response = await fetch("AddJob");
  const data = await response.json();
  updateCompanyNav(data);
}

async function submitAddJob(event) {
  event.preventDefault();

  const form = event.currentTarget;
  const params = new URLSearchParams();
  for (const [key, value] of new FormData(form).entries()) {
    params.append(key, value);
  }

  const response = await fetch("AddJob", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8"
    },
    body: params.toString()
  });
  const data = await response.json();

  const errorBox = document.getElementById("add-job-error");
  const messageBox = document.getElementById("add-job-message");
  errorBox.style.display = "none";
  messageBox.style.display = "none";

  if (data.success) {
    messageBox.textContent = data.message;
    messageBox.style.display = "block";
    form.reset();
  } else {
    errorBox.textContent = data.error || "求人登録に失敗しました。";
    errorBox.style.display = "block";
  }
}

document.addEventListener("DOMContentLoaded", () => {
  loadState().catch(() => {
    const errorBox = document.getElementById("add-job-error");
    errorBox.textContent = "初期表示の取得に失敗しました。";
    errorBox.style.display = "block";
  });

  document.getElementById("add-job-form").addEventListener("submit", submitAddJob);
});
