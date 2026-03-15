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

function formatDate(value) {
  if (!value) {
    return "";
  }
  return value.slice(0, 10).replace(/-/g, "/");
}

function roleLabel(role) {
  if (role === "job_seeker") return "求職者";
  if (role === "company") return "企業";
  return role || "未ログイン";
}

function statusLabel(status) {
  if (status === "applied") return "応募済み";
  if (status === "screening") return "選考中";
  if (status === "rejected") return "不採用";
  if (status === "accepted") return "採用";
  return status || "";
}

function updateNav(data) {
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
    }
    right.innerHTML = `
      <span class="nav-user-text">ログイン中: ${escapeHtml(data.currentName)}</span>
      <a href="${contextPath}/logout.html">logout</a>
    `;
  }

  left.innerHTML = leftHtml;
}

async function loadMy() {
  const response = await fetch("My");
  const data = await response.json();
  updateNav(data);

  const errorBox = document.getElementById("my-error");
  if (data.error) {
    errorBox.textContent = data.error;
    errorBox.style.display = "block";
  } else {
    errorBox.style.display = "none";
  }

  document.getElementById("my-name").textContent = data.currentName || "ゲスト";
  document.getElementById("my-role").textContent = roleLabel(data.currentRole);

  const body = document.getElementById("history-body");
  const contextPath = window.location.pathname.split("/").slice(0, 2).join("/");

  if (!data.history || data.history.length === 0) {
    body.innerHTML = `<tr><td colspan="3">応募履歴はまだありません。</td></tr>`;
    return;
  }

  body.innerHTML = data.history.map((item) => `
    <tr>
      <td><a href="${contextPath}/job_detail.html?id=${item.jobId}">${escapeHtml(item.title)}</a></td>
      <td>${escapeHtml(formatDate(item.appliedAt))}</td>
      <td>${escapeHtml(statusLabel(item.status))}</td>
    </tr>
  `).join("");
}

document.addEventListener("DOMContentLoaded", () => {
  loadMy().catch(() => {
    const errorBox = document.getElementById("my-error");
    errorBox.textContent = "個人ページの取得に失敗しました。";
    errorBox.style.display = "block";
  });
});
