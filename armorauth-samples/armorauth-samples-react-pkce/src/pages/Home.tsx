import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getUser, login, logout } from '../auth';
import type { User } from 'oidc-client-ts';

function Home() {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    getUser().then((u) => {
      setUser(u);
      setLoading(false);
      if (u && !u.expired) {
        navigate('/dashboard');
      }
    });
  }, [navigate]);

  if (loading) {
    return <div className="container">Loading...</div>;
  }

  return (
    <div className="container">
      <h1>ArmorAuth React PKCE Sample</h1>
      <p>This sample demonstrates the Authorization Code + PKCE flow with a React SPA.</p>

      {user && !user.expired ? (
        <div>
          <p>Welcome, {user.profile.sub}!</p>
          <button onClick={() => navigate('/dashboard')}>Go to Dashboard</button>
          <button onClick={logout} className="secondary">Logout</button>
        </div>
      ) : (
        <div>
          <button onClick={login}>Login with ArmorAuth</button>
        </div>
      )}
    </div>
  );
}

export default Home;
