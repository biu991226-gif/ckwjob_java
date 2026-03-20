function escapeHtml(value) {
  if (value == null) return "";
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function formatDate(value) {
  if (!value) return "";
  return value.slice(0, 10).replace(/-/g, "/");
}

function statusLabel(status) {
  if (status === "applied") return "応募済み";
  if (status === "screening") return "選考中";
  if (status === "rejected") return "不採用";
  if (status === "accepted") return "採用";
  return status || "";
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

async function loadApplicants() {
  const params = new URLSearchParams(window.location.search);
  const id = params.get("id") || "";
  const response = await fetch(`JobApplicants?${new URLSearchParams({ id })}`);
  const data = await response.json();
  updateCompanyNav(data);

  document.getElementById("applicants-job-title").textContent = data.jobTitle || id;

  const errorBox = document.getElementById("applicants-error");
  if (data.error) {
    errorBox.textContent = data.error;
    errorBox.style.display = "block";
  } else {
    errorBox.style.display = "none";
  }

  const body = document.getElementById("applicants-body");
  if (!data.error && (!data.applicants || data.applicants.length === 0)) {
    body.innerHTML = `<tr><td colspan="5">応募者はまだいません。</td></tr>`;
    return;
  }

  body.innerHTML = (data.applicants || []).map((applicant) => `
    <tr>
      <td>${escapeHtml(applicant.name)}</td>
      <td>${escapeHtml(applicant.email)}</td>
      <td>${escapeHtml(applicant.phone)}</td>
      <td>${escapeHtml(formatDate(applicant.appliedAt))}</td>
      <td>${escapeHtml(statusLabel(applicant.status))}</td>
    </tr>
  `).join("");
}

document.addEventListener("DOMContentLoaded", () => {
  loadApplicants().catch(() => {
    const errorBox = document.getElementById("applicants-error");
    errorBox.textContent = "応募者一覧の取得に失敗しました。";
    errorBox.style.display = "block";
  });
});
