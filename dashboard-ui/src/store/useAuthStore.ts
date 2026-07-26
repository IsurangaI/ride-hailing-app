import { create } from 'zustand';

interface AuthState {
    token: string | null;
    role: string | null;
    setAuth: (token: string, role: string) => void;
    logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
    token: localStorage.getItem('jwt_token'),
    role: localStorage.getItem('user_role'),
    setAuth: (token, role) => {
        localStorage.setItem('jwt_token', token);
        localStorage.setItem('user_role', role);
        set({ token, role });
    },
    logout: () => {
        localStorage.removeItem('jwt_token');
        localStorage.removeItem('user_role');
        set({ token: null, role: null });
    },
}));