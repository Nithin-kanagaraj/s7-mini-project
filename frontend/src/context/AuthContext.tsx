import React, { createContext, useContext, useState, useEffect } from 'react';

export interface User {
  id: string;
  username: string;
  role: 'ADMIN' | 'HR' | 'SCHEDULER' | 'DEPT_HEAD' | 'WORKER';
  employeeId?: string;
  email?: string;
  fullName?: string;
  departmentId?: string;
  departmentName?: string;
}

interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<User>;
  logout: () => Promise<void>;
  fetchMe: () => Promise<User | null>;
  hasRole: (...roles: string[]) => boolean;
  isAdmin: boolean;
  isHr: boolean;
  isScheduler: boolean;
  isDeptHead: boolean;
  isWorker: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  const fetchMe = async (): Promise<User | null> => {
    try {
      const token = localStorage.getItem('access_token');
      const headers: Record<string, string> = { 'Content-Type': 'application/json' };
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      const res = await fetch('/api/auth/me', { headers });
      if (res.ok) {
        const data = await res.json();
        const userData: User = {
          id: data.id,
          username: data.username,
          role: data.role,
          employeeId: data.employeeId,
          email: data.email,
          fullName: data.fullName,
          departmentId: data.departmentId,
          departmentName: data.departmentName,
        };
        setUser(userData);
        return userData;
      } else {
        setUser(null);
        localStorage.removeItem('access_token');
        return null;
      }
    } catch {
      setUser(null);
      return null;
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMe();
  }, []);

  const login = async (username: string, password: string): Promise<User> => {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: 'Authentication failed' }));
      throw new Error(err.message || 'Invalid credentials');
    }

    const data = await res.json();
    if (data.accessToken) {
      localStorage.setItem('access_token', data.accessToken);
    }

    const userData: User = {
      id: data.user.id,
      username: data.user.username,
      role: data.user.role,
      employeeId: data.user.employeeId,
      email: data.user.email,
      fullName: data.user.fullName,
      departmentId: data.user.departmentId,
      departmentName: data.user.departmentName,
    };

    setUser(userData);
    return userData;
  };

  const logout = async (): Promise<void> => {
    try {
      const token = localStorage.getItem('access_token');
      await fetch('/api/auth/logout', {
        method: 'POST',
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
    } catch (e) {
      console.error('Logout error', e);
    } finally {
      localStorage.removeItem('access_token');
      setUser(null);
    }
  };

  const hasRole = (...roles: string[]) => {
    if (!user) return false;
    return roles.includes(user.role);
  };

  const isAdmin = user?.role === 'ADMIN';
  const isHr = user?.role === 'HR';
  const isScheduler = user?.role === 'SCHEDULER';
  const isDeptHead = user?.role === 'DEPT_HEAD';
  const isWorker = user?.role === 'WORKER';

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        login,
        logout,
        fetchMe,
        hasRole,
        isAdmin,
        isHr,
        isScheduler,
        isDeptHead,
        isWorker,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
