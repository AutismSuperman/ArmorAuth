import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { loginCallback } from '../auth';

function Callback() {
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    loginCallback()
      .then(() => {
        navigate('/dashboard');
      })
      .catch((err) => {
        setError(err.message || 'Login failed');
      });
  }, [navigate]);

  if (error) {
    return (
      <div className="container">
        <h2>Login Error</h2>
        <p className="error">{error}</p>
        <button onClick={() => navigate('/')}>Back to Home</button>
      </div>
    );
  }

  return <div className="container">Processing login...</div>;
}

export default Callback;
