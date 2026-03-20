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

function showMessages(data) {
  const errorBox = document.getElementById("manage-error");
  const messageBox = document.getElementById("manage-message");

  if (data.error) {
    errorBox.textContent = data.error;
    errorBox.style.display = "block";
  } else {
    errorBox.style.display = "none";
  }

  if (data.message) {
    messageBox.textContent = data.message;
    messageBox.style.display = "block";
  } else {
    messageBox.style.display = "none";
  }
}

function renderJobs(data) {
  const body = document.getElementById("manage-jobs-body");
  const contextPath = window.location.pathname.split("/").slice(0, 2).join("/");

  if (!data.jobs || data.jobs.length === 0) {
    body.innerHTML = `<tr><td colspan="7">登録済みの求人はありません。</td></tr>`;
    return;
  }

  body.innerHTML = data.jobs.map((job) => `
    <tr>
      <td>${escapeHtml(job.title)}</td>
      <td>${escapeHtml(job.area)}</td>
      <td>
        <form class="status-form" data-job-id="${job.id}">
          <select name="new_status" class="xx status-select">
            <option value="open" ${job.status === "open" ? "selected" : ""}>募集中</option>
            <option value="closed" ${job.status === "closed" ? "selected" : ""}>募集終了</option>
          </select>
          <input type="submit" value="更新" class="x4">
        </form>
      </td>
      <td>${escapeHtml(formatDate(job.createdAt))}</td>
      <td>${job.applicantCount}</td>
      <td><a href="${contextPath}/job_applicants.html?id=${job.id}">一覧</a></td>
      <td>
        <form class="delete-form" data-job-id="${job.id}">
          <input type="submit" value="削除" class="x4">
        </form>
      </td>
    </tr>
  `).join("");

  for (const form of document.querySelectorAll(".status-form")) {
    form.addEventListener("submit", submitStatusUpdate);
  }
  for (const form of document.querySelectorAll(".delete-form")) {
    form.addEventListener("submit", submitDelete);
  }
}

async function loadManageJobs() {
  const response = await fetch("ManageJobs");
  const data = await response.json();
  updateCompanyNav(data);
  showMessages(data);
  renderJobs(data);
}

async function postManage(action, payload) {
  const params = new URLSearchParams(payload);
  params.set("action", action);

  const response = await fetch("ManageJobs", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8"
    },
    body: params.toString()
  });
  const data = await response.json();
  updateCompanyNav(data);
  showMessages(data);
  renderJobs(data);
}

function submitStatusUpdate(event) {
  event.preventDefault();
  const form = event.currentTarget;
  postManage("update_status", {
    job_id: form.dataset.jobId,
    new_status: form.querySelector("select").value
  });
}

function submitDelete(event) {
  event.preventDefault();
  if (!window.confirm("この求人を削除しますか？")) {
    return;
  }
  const form = event.currentTarget;
  postManage("delete", {
    job_id: form.dataset.jobId
  });
}

document.addEventListener("DOMContentLoaded", () => {
  loadManageJobs().catch(() => {
    const errorBox = document.getElementById("manage-error");
    errorBox.textContent = "求人管理の取得に失敗しました。";
    errorBox.style.display = "block";
  });
});
