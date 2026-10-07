import React from 'react';
import { useAuth } from '../context/AuthContext';
import { NotificationCenter } from './NotificationCenter';
import { LogOut, User as UserIcon, Building2, Shield } from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, logout } = useAuth();

  const getRoleBadgeClass = (role?: string) => {
    switch (role) {
      case 'ADMIN': return 'bg-rose-100 text-rose-800 border-rose-200';
      case 'HR': return 'bg-purple-100 text-purple-800 border-purple-200';
      case 'SCHEDULER': return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'DEPT_HEAD': return 'bg-amber-100 text-amber-800 border-amber-200';
      default: return 'bg-emerald-100 text-emerald-800 border-emerald-200';
    }
  };

  return (
    <header className="sticky top-0 z-30 bg-white/95 backdrop-blur border-b border-slate-200 shadow-xs px-6 py-3.5 flex items-center justify-between">
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-sky-600 to-cyan-500 flex items-center justify-center text-white font-bold shadow-md shadow-sky-500/20">
          <Building2 className="w-5 h-5" />
        </div>
        <div>
          <h1 className="text-lg font-bold text-slate-900 leading-tight">MetroCare Hospital</h1>
          <p className="text-xs font-medium text-slate-500">Shift Scheduling & Workforce System</p>
        </div>
      </div>

      {user && (
        <div className="flex items-center gap-4">
          <NotificationCenter />

          <div className="flex items-center gap-3 px-3.5 py-1.5 rounded-lg bg-slate-50 border border-slate-200">
            <div className="w-8 h-8 rounded-full bg-slate-200 flex items-center justify-center text-slate-700 font-semibold text-xs">
              <UserIcon className="w-4 h-4" />
            </div>
            <div className="text-left">
              <div className="flex items-center gap-2">
                <span className="text-sm font-semibold text-slate-800">{user.fullName || user.username}</span>
                <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full border ${getRoleBadgeClass(user.role)}`}>
                  {user.role}
                </span>
              </div>
              {user.departmentName && (
                <div className="flex items-center gap-1 text-[11px] text-slate-500 font-medium">
                  <Shield className="w-3 h-3 text-slate-400" />
                  <span>{user.departmentName}</span>
                </div>
              )}
            </div>
          </div>

          <button
            onClick={logout}
            className="flex items-center gap-2 px-3.5 py-2 text-sm font-medium text-slate-600 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors cursor-pointer"
            title="Sign Out"
          >
            <LogOut className="w-4 h-4" />
            <span>Sign Out</span>
          </button>
        </div>
      )}
    </header>
  );
};
