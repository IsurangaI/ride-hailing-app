import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import apiClient from '../api/apiClient';
import { useAuthStore } from '../store/useAuthStore';

function parseJwtRole(token: string): string {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.role;
}

function Login() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const { setAuth } = useAuthStore();
    const navigate = useNavigate();

    const handleSubmit = async (e: { preventDefault(): void }) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        try {
            const { data: token } = await apiClient.post<string>('/api/auth/login', { email, password });
            const role = parseJwtRole(token);
            setAuth(token, role);
            navigate(role === 'DRIVER' ? '/driver' : '/rider');
        } catch (err: any) {
            setError(err.response?.data?.message || err.response?.data || 'Invalid credentials');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            <h2>Login</h2>
            <form onSubmit={handleSubmit}>
                <div>
                    <label>Email</label><br />
                    <input
                        type="email"
                        value={email}
                        onChange={e => setEmail(e.target.value)}
                        required
                    />
                </div>
                <div>
                    <label>Password</label><br />
                    <input
                        type="password"
                        value={password}
                        onChange={e => setPassword(e.target.value)}
                        required
                    />
                </div>
                {error && <p style={{ color: 'red' }}>{error}</p>}
                <button type="submit" disabled={loading}>
                    {loading ? 'Logging in...' : 'Login'}
                </button>
            </form>
            <p>Don't have an account? <Link to="/register">Register</Link></p>
        </div>
    );
}

export default Login;
