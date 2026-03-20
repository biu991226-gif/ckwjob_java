async function runLogout() {
  const message = document.getElementById("logout-message");

  try {
    const response = await fetch("Logout");
    const data = await response.json();
    message.textContent = "ログアウトしました。トップへ移動します。";
    if (data.redirectUrl) {
      window.setTimeout(() => {
        window.location.href = data.redirectUrl;
      }, 600);
    }
  } catch (error) {
    message.textContent = "ログアウト処理に失敗しました。";
  }
}

document.addEventListener("DOMContentLoaded", runLogout);
