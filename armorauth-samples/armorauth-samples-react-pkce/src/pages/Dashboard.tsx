import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getUser, logout, getAccessToken } from '../auth';
import type { User } from 'oidc-client-ts';

interface UserInfo {
  sub?: string;
  name?: string;
  email?: string;
  preferred_username?: string;
  roles?: string[];
  [key: string]: unknown;
}

function Dashboard() {
  const [user, setUser] = useState<User | null>(null);
  const [userInfo, setUserInfo] = useState<UserInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    getUser().then((u) => {
      if (!u || u.expired) {
        navigate('/');
        return;
      }
      setUser(u);
      setLoading(false);

      // Fetch userinfo from the server
      getAccessToken().then((token) => {
        if (token) {
          fetch('http://localhost:9000/userinfo', {
            headers: { Authorization: `Bearer ${token}` },
          })
            .then((res) => res.json())
            .then((data: UserInfo) => setUserInfo(data))
            .catch(console.error);
        }
      });
    });
  }, [navigate]);

  if (loading) {
    return <div className="container">Loading...</div>;
  }

  return (
    <div className="container">
      <h1>Dashboard</h1>
      <div className="card">
        <h2>ID Token Claims</h2>
        <pre>{JSON.stringify(user?.profile, null, 2)}</pre>
      </div>

      {userInfo && (
        <div className="card">
          <h2>UserInfo Endpoint</h2>
          <pre>{JSON.stringify(userInfo, null, 2)}</pre>
        </div>
      )}

      <div className="card">
        <h2>Tokens</h2>
        <p><strong>Access Token:</strong> {user?.access_token?.substring(0, 50)}...</p>
        <p><strong>Expires At:</strong> {user?.expires_at ? new Date(user.expires_at * 1000).toLocaleString() : 'N/A'}</p>
        <p><strong>Scopes:</strong> {user?.scope}</p>
      </div>

      <button onClick={logout}>Logout</button>
    </div>
  );
}

export default Dashboard;
