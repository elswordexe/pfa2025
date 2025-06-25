export const parseJwt = (token) => {
  if (!token) return null;
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%'.concat(('00' + c.charCodeAt(0).toString(16)).slice(-2)))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch (e) {
    console.error('Error parsing JWT', e);
    return null;
  }
};

export const getRoleFromToken = () => {
  const token = localStorage.getItem('token');
  const decoded = parseJwt(token);
  return decoded?.role || null;
};

export const getUserIdFromToken = () => {
  const token = localStorage.getItem('token');
  const decoded = parseJwt(token);
  return decoded?.id || null;
};

export const isTokenExpired = () => {
  const token = localStorage.getItem('token');
  if (!token) return true;
  const decoded = parseJwt(token);
  if (!decoded || !decoded.exp) return true;
  // exp est en secondes
  const isExpired = decoded.exp * 1000 < Date.now();
  return isExpired;
}; 