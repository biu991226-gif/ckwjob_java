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

function nl2br(value) {
  return escapeHtml(value).replace(/\n/g, "<br>");
}

function formatDate(value) {
  if (!value) {
    return "";
  }
  return value.slice(0, 10).replace(/-/g, "/");
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

function renderApply(data) {
  const container = document.getElementById("apply-cell");
  const contextPath = window.location.pathname.split("/").slice(0, 2).join("/");
  const job = data.job;

  if (!data.isLoggedIn) {
    container.innerHTML = `応募するには <a href="${contextPath}/login.html">ログイン</a> してください。`;
    return;
  }
  if (data.currentRole !== "job_seeker") {
    container.textContent = "企業アカウントでは応募できません。";
    return;
  }
  if (job.status !== "open") {
    container.textContent = "この求人は現在応募を受け付けていません。";
    return;
  }
  if (data.isAlreadyApplied) {
    container.innerHTML = `<span class="status-applied">応募済み</span>です。`;
    return;
  }

  container.innerHTML = `
    <a href="${contextPath}/apply.html?job_id=${job.id}" class="apply-action-link">応募する</a>
  `;
}

async function loadJobDetail() {
  const params = new URLSearchParams(window.location.search);
  const id = params.get("id") || "";
  const response = await fetch(`JobDetail?${new URLSearchParams({ id })}`);
  const data = await response.json();
  updateNav(data);

  const errorBox = document.getElementById("detail-error");
  if (data.dbError) {
    errorBox.textContent = data.dbError;
    errorBox.style.display = "block";
  } else {
    errorBox.style.display = "none";
  }

  if (!data.dbError && !data.job) {
    errorBox.textContent = "指定された求人は見つかりませんでした。";
    errorBox.style.display = "block";
    document.getElementById("detail-table").style.display = "none";
    return;
  }

  if (!data.job) {
    document.getElementById("detail-table").style.display = "none";
    return;
  }

  const job = data.job;
  document.getElementById("job-title").textContent = job.title;
  document.getElementById("job-salary").textContent = job.salary;
  document.getElementById("job-area").textContent = job.area;
  document.getElementById("job-created-at").textContent = formatDate(job.createdAt);
  document.getElementById("job-status").textContent = job.status;
  document.getElementById("job-description").innerHTML = nl2br(job.description);
  renderApply(data);
}

document.addEventListener("DOMContentLoaded", () => {
  loadJobDetail().catch(() => {
    const errorBox = document.getElementById("detail-error");
    errorBox.textContent = "求人詳細の取得に失敗しました。";
    errorBox.style.display = "block";
  });
});
