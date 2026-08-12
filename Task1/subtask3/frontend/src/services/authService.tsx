export async function checkAuthentication(): Promise<boolean> {
  try {
    const response = await fetch(`${process.env.REACT_APP_AUTH_URL}/session`, {
      credentials: 'include', // отправляем cookie
    });
    return response.ok;
  } catch {
    return false;
  }
}

export function redirectToLogin(): void {
  window.location.href = `${process.env.REACT_APP_AUTH_URL}/login`;
}
