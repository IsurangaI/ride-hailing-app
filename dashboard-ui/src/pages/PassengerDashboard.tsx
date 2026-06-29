import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/useAuthStore';

function PassengerDashboard() {
    const { logout } = useAuthStore();
    const navigate = useNavigate();

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    return (
        <div>
            <h2>Rider Dashboard</h2>
            <button onClick={handleLogout}>Logout</button>
        </div>
    );
}

export default PassengerDashboard;
