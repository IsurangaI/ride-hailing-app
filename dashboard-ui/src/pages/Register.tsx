import apiClient from '../api/apiClient';
import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';

function Register() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [role, setRole] = useState('PASSENGER');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    const handleSubmit = async (e: { preventDefault(): void }) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        try {
            await apiClient.post('/api/auth/register', { email, password, role });
            navigate('/login');
        } catch (err: any) {
            setError(err.response?.data?.message || err.response?.data || 'Registration failed');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            <h2>Register</h2>
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
                <div>
                    <label>Role</label><br />
                    <select value={role} onChange={e => setRole(e.target.value)}>
                        <option value="PASSENGER">Passenger</option>
                        <option value="DRIVER">Driver</option>
                    </select>
                </div>
                {error && <p style={{ color: 'red' }}>{error}</p>}
                <button type="submit" disabled={loading}>
                    {loading ? 'Registering...' : 'Register'}
                </button>
            </form>
            <p>Already have an account? <Link to="/login">Login</Link></p>
        </div>
    );
}

export default Register;
