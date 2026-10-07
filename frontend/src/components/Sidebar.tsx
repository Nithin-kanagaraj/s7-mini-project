import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Users, Building, Award, Clock, Layers, Calendar, FileText, LayoutDashboard, CalendarDays, ShieldCheck, Shield, BarChart3, UserCheck } from 'lucide-react';

export const Sidebar: React.FC = () => {
  const { user, isAdmin, isHr, isScheduler, isDeptHead } = useAuth();

  const navItemClass = ({ isActive }: { isActive: boolean }) =>
    `flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-medium transition-all ${
      isActive
        ? 'bg-sky-600 text-white shadow-md shadow-sky-600/20'
        : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
    }`;

  return (
    <aside className="w-64 bg-white border-r border-slate-200 p-4 flex flex-col justify-between min-h-[calc(100vh-65px)]">
      <div className="space-y-6">
        {/* Main Dashboard Section */}
        <div>
          <div className="px-3 mb-2 text-[11px] font-bold tracking-wider text-slate-400 uppercase">
            Overview
          </div>
          <nav className="space-y-1">
            <NavLink to="/dashboard" className={navItemClass}>
              <LayoutDashboard className="w-4 h-4" />
              <span>Dashboard</span>
            </NavLink>

            {(isAdmin || isHr || isScheduler || isDeptHead) && (
              <NavLink to="/reports" className={navItemClass}>
                <BarChart3 className="w-4 h-4" />
                <span>Reports & Analytics</span>
              </NavLink>
            )}

            {isAdmin && (
              <NavLink to="/audit-logs" className={navItemClass}>
                <Shield className="w-4 h-4" />
                <span>Audit Log Explorer</span>
              </NavLink>
            )}
          </nav>
        </div>

        {/* Schedule Engine & Roster */}
        <div>
          <div className="px-3 mb-2 text-[11px] font-bold tracking-wider text-slate-400 uppercase">
            Shift Scheduling Engine
          </div>
          <nav className="space-y-1">
            {(isAdmin || isScheduler || isDeptHead || isHr) && (
              <NavLink to="/schedules" className={navItemClass}>
                <CalendarDays className="w-4 h-4" />
                <span>Schedule Builder</span>
              </NavLink>
            )}

            <NavLink to="/my-schedule" className={navItemClass}>
              <Calendar className="w-4 h-4" />
              <span>My Shift Roster</span>
            </NavLink>

            {(isAdmin || isHr || isDeptHead) && (
              <NavLink to="/attendance" className={navItemClass}>
                <UserCheck className="w-4 h-4" />
                <span>Attendance Log</span>
              </NavLink>
            )}

            {isAdmin && (
              <NavLink to="/compliance" className={navItemClass}>
                <ShieldCheck className="w-4 h-4" />
                <span>Labor Compliance</span>
              </NavLink>
            )}
          </nav>
        </div>

        {/* Core Management Modules */}
        <div>
          <div className="px-3 mb-2 text-[11px] font-bold tracking-wider text-slate-400 uppercase">
            Core Modules
          </div>
          <nav className="space-y-1">
            <NavLink to="/employees" className={navItemClass}>
              <Users className="w-4 h-4" />
              <span>Employees</span>
            </NavLink>

            {(isAdmin || isHr || isScheduler || isDeptHead) && (
              <NavLink to="/departments" className={navItemClass}>
                <Building className="w-4 h-4" />
                <span>Departments</span>
              </NavLink>
            )}

            <NavLink to="/skills" className={navItemClass}>
              <Award className="w-4 h-4" />
              <span>Skills Registry</span>
            </NavLink>

            <NavLink to="/shift-templates" className={navItemClass}>
              <Clock className="w-4 h-4" />
              <span>Shift Templates</span>
            </NavLink>

            {(isAdmin || isHr || isScheduler || isDeptHead) && (
              <NavLink to="/staffing-requirements" className={navItemClass}>
                <Layers className="w-4 h-4" />
                <span>Staffing Requirements</span>
              </NavLink>
            )}
          </nav>
        </div>

        {/* Workforce & Availability */}
        <div>
          <div className="px-3 mb-2 text-[11px] font-bold tracking-wider text-slate-400 uppercase">
            Workforce & Leave
          </div>
          <nav className="space-y-1">
            <NavLink to="/availability" className={navItemClass}>
              <Calendar className="w-4 h-4" />
              <span>My Availability</span>
            </NavLink>

            <NavLink to="/leave" className={navItemClass}>
              <FileText className="w-4 h-4" />
              <span>Leave Requests</span>
            </NavLink>
          </nav>
        </div>
      </div>

      {user && (
        <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-500">
          <div className="font-semibold text-slate-700 mb-1">System Status</div>
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
            <span>MySQL & WebSocket Connected</span>
          </div>
        </div>
      )}
    </aside>
  );
};
