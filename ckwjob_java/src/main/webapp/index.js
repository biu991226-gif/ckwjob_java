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

function updateNav(data) {
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
      <span class="nav-user-text">ログイン中: ${escapeHtml(data.currentName)}</span>
      <a href="${contextPath}/logout.html">ログアウト</a>
    `;
  } else {
    leftHtml += `<a href="${contextPath}/login.html">ログイン / 会員登録</a>`;
    right.innerHTML = "";
  }

  left.innerHTML = leftHtml;
}

function renderJobs(data) {
  const container = document.getElementById("jobs-container");
  if (!data.jobs || data.jobs.length === 0) {
    container.innerHTML = "該当する求人はありません。<br>";
    return;
  }

  const contextPath = window.location.pathname.split("/").slice(0, 2).join("/");
  container.innerHTML = data.jobs.map((job) => `
    <div class="job-item">
      <a href="${contextPath}/job_detail.html?id=${job.id}" class="job-title">${escapeHtml(job.title)}</a>
      <span class="job-meta">${escapeHtml(job.area)}</span>
      <span class="job-meta">${escapeHtml(job.salary)}</span>
    </div>
  `).join("");
}

async function loadIndex() {
  const params = new URLSearchParams(window.location.search);
  const keyword = params.get("keyword") || "";
  document.getElementById("keyword").value = keyword;

  try {
    const response = await fetch(`Index?${new URLSearchParams({ keyword })}`);
    const data = await response.json();
    updateNav(data);

    const notice = document.getElementById("login-notice");
    notice.style.display = data.isLoggedIn ? "none" : "block";

    const errorBox = document.getElementById("db-error");
    if (data.dbError) {
      errorBox.textContent = data.dbError;
      errorBox.style.display = "block";
    } else {
      errorBox.style.display = "none";
    }

    renderJobs(data);
  } catch (error) {
    document.getElementById("db-error").textContent = "データ取得に失敗しました。";
    document.getElementById("db-error").style.display = "block";
  }
}

document.addEventListener("DOMContentLoaded", loadIndex);
