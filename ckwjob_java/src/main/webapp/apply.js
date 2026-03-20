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

async function runApply() {
  const params = new URLSearchParams(window.location.search);
  const jobId = params.get("job_id") || "";
  const messageBox = document.getElementById("apply-message");
  const resultText = document.getElementById("apply-result-text");

  if (!jobId) {
    messageBox.textContent = "求人IDが不正です。";
    messageBox.className = "message-error";
    resultText.textContent = "応募は完了していません。内容を確認してください。";
    return;
  }

  try {
    const body = new URLSearchParams({ job_id: jobId });
    const response = await fetch("Apply", {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8"
      },
      body: body.toString()
    });
    const data = await response.json();

    if (data.success) {
      messageBox.textContent = data.message;
      messageBox.className = "message-success";
      resultText.textContent = "応募が完了しました。個人ページで応募状況を確認できます。";
    } else {
      messageBox.textContent = data.error || "応募処理に失敗しました。";
      messageBox.className = "message-error";
      resultText.textContent = "応募は完了していません。内容を確認してください。";
    }
  } catch (error) {
    messageBox.textContent = "応募処理に失敗しました。";
    messageBox.className = "message-error";
    resultText.textContent = "応募は完了していません。内容を確認してください。";
  }
}

document.addEventListener("DOMContentLoaded", runApply);
