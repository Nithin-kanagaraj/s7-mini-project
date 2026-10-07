import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { Link } from 'react-router-dom';
import { 
  Users, Building, Calendar, AlertTriangle, CheckCircle2, Clock, 
  TrendingUp, Shield, FileText, ArrowRight, Award, Layers
} from 'lucide-react';

interface StatsOverview {
  totalEmployees: number;
  totalDepartments: number;
  activeSchedules: number;
  pendingLeaves: number;
  shortageCount: number;
}

interface PersonalShift {
  id: string;
  shiftTemplateName: string;
  shiftStartTime: string;
  shiftEndTime: string;
  assignmentDate: string;
  departmentName: string;
  isOvertime: boolean;
}

export const DashboardPage: React.FC = () => {
  const { user, isAdmin, isHr, isScheduler, isDeptHead, isWorker } = useAuth();
  const [stats, setStats] = useState<StatsOverview>({
    totalEmployees: 0,
    totalDepartments: 0,
    activeSchedules: 0,
    pendingLeaves: 0,
    shortageCount: 0,
  });
  const [myNextShifts, setMyNextShifts] = useState<PersonalShift[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchDashboardData = async () => {
      setLoading(true);
      try {
        const token = localStorage.getItem('access_token');
        const headers = { Authorization: `Bearer ${token}` };

        // Fetch Stats
        const [empRes, deptRes, leaveRes, schedRes] = await Promise.all([
          fetch('/api/employees?size=1', { headers }).catch(() => null),
          fetch('/api/departments', { headers }).catch(() => null),
          fetch('/api/leave', { headers }).catch(() => null),
          fetch('/api/schedules', { headers }).catch(() => null),
        ]);

        let empTotal = 0;
        let deptTotal = 0;
        let pendingLeaveTotal = 0;
        let scheduleCount = 0;
        let shortages = 0;

        if (empRes?.ok) {
          const empData = await empRes.json();
          empTotal = empData.totalElements || 0;
        }

        if (deptRes?.ok) {
          const depts = await deptRes.json();
          deptTotal = depts.length || 0;
        }

        if (leaveRes?.ok) {
          const leaves = await leaveRes.json();
          pendingLeaveTotal = leaves.filter((l: any) => l.status === 'PENDING').length;
        }

        if (schedRes?.ok) {
          const scheds = await schedRes.json();
          scheduleCount = scheds.length;
          shortages = scheds.filter((s: any) => s.hasShortages).length;
        }

        setStats({
          totalEmployees: empTotal,
          totalDepartments: deptTotal,
          activeSchedules: scheduleCount,
          pendingLeaves: pendingLeaveTotal,
          shortageCount: shortages,
        });

        // If Worker, fetch personal assignments
        if (isWorker || user?.employeeId) {
          const myRes = await fetch('/api/schedules/my-assignments', { headers }).catch(() => null);
          if (myRes?.ok) {
            const myAssignments = await myRes.json();
            const upcoming = myAssignments
              .filter((a: any) => new Date(a.assignmentDate) >= new Date(new Date().setHours(0,0,0,0)))
              .slice(0, 5);
            setMyNextShifts(upcoming);
          }
        }
      } catch (e) {
        console.error('Failed to load dashboard statistics', e);
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
  }, [user, isWorker]);

  return (
    <div className="space-y-8">
      {/* Welcome Banner */}
      <div className="glass-panel p-6 rounded-2xl bg-gradient-to-r from-slate-900 via-slate-800 to-sky-950 text-white shadow-xl relative overflow-hidden">
        <div className="relative z-10 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-sky-500/20 border border-sky-400/30 text-sky-300 text-xs font-semibold mb-3">
              <Shield className="w-3.5 h-3.5" />
              <span>Role Context: {user?.role}</span>
            </div>
            <h2 className="text-2xl font-bold tracking-tight">
              Welcome back, {user?.fullName || user?.username}!
            </h2>
            <p className="text-slate-300 text-sm mt-1 max-w-xl">
              {isAdmin && 'System Administration & Global Compliance Control Dashboard.'}
              {isHr && 'Workforce Management, Employee Profiles & Certification Compliance.'}
              {isScheduler && 'Constraint-based Shift Scheduling Engine & Roster Optimization.'}
              {isDeptHead && `Department Operations Hub — ${user?.departmentName || 'Department Head'}.`}
              {isWorker && 'Personal Roster, Upcoming Shifts & Leave Management.'}
            </p>
          </div>
          <div className="flex gap-3">
            {(isAdmin || isScheduler) && (
              <Link
                to="/schedules"
                className="px-4 py-2.5 bg-sky-500 hover:bg-sky-400 text-white text-sm font-semibold rounded-xl shadow-lg shadow-sky-500/30 transition-all flex items-center gap-2"
              >
                <Calendar className="w-4 h-4" />
                <span>Open Schedule Builder</span>
              </Link>
            )}
            {isWorker && (
              <Link
                to="/my-schedule"
                className="px-4 py-2.5 bg-sky-500 hover:bg-sky-400 text-white text-sm font-semibold rounded-xl shadow-lg shadow-sky-500/30 transition-all flex items-center gap-2"
              >
                <Calendar className="w-4 h-4" />
                <span>View My Roster</span>
              </Link>
            )}
          </div>
        </div>
      </div>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <div className="glass-card p-5 rounded-2xl flex items-center justify-between">
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Total Active Staff</p>
            <h3 className="text-2xl font-bold text-slate-900 mt-1">{loading ? '...' : stats.totalEmployees}</h3>
            <p className="text-xs text-emerald-600 font-medium mt-1 flex items-center gap-1">
              <TrendingUp className="w-3.5 h-3.5" /> Active Workforce
            </p>
          </div>
          <div className="w-12 h-12 rounded-xl bg-sky-100 text-sky-600 flex items-center justify-center font-bold">
            <Users className="w-6 h-6" />
          </div>
        </div>

        <div className="glass-card p-5 rounded-2xl flex items-center justify-between">
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Hospital Units</p>
            <h3 className="text-2xl font-bold text-slate-900 mt-1">{loading ? '...' : stats.totalDepartments}</h3>
            <p className="text-xs text-slate-500 font-medium mt-1">Operational Departments</p>
          </div>
          <div className="w-12 h-12 rounded-xl bg-purple-100 text-purple-600 flex items-center justify-center font-bold">
            <Building className="w-6 h-6" />
          </div>
        </div>

        <div className="glass-card p-5 rounded-2xl flex items-center justify-between">
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Active Schedules</p>
            <h3 className="text-2xl font-bold text-slate-900 mt-1">{loading ? '...' : stats.activeSchedules}</h3>
            <p className="text-xs text-amber-600 font-medium mt-1 flex items-center gap-1">
              {stats.shortageCount > 0 && <AlertTriangle className="w-3.5 h-3.5" />}
              {stats.shortageCount > 0 ? `${stats.shortageCount} with Shortages` : 'Fully Staffed'}
            </p>
          </div>
          <div className="w-12 h-12 rounded-xl bg-emerald-100 text-emerald-600 flex items-center justify-center font-bold">
            <Calendar className="w-6 h-6" />
          </div>
        </div>

        <div className="glass-card p-5 rounded-2xl flex items-center justify-between">
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Pending Leaves</p>
            <h3 className="text-2xl font-bold text-slate-900 mt-1">{loading ? '...' : stats.pendingLeaves}</h3>
            <p className="text-xs text-slate-500 font-medium mt-1">Awaiting Approval</p>
          </div>
          <div className="w-12 h-12 rounded-xl bg-amber-100 text-amber-600 flex items-center justify-center font-bold">
            <FileText className="w-6 h-6" />
          </div>
        </div>
      </div>

      {/* Role-tailored Section Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column (2 spans): Primary Actions & Roster */}
        <div className="lg:col-span-2 space-y-6">
          {(isAdmin || isHr) && (
            <div className="glass-card p-6 rounded-2xl space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
                  <Shield className="w-5 h-5 text-sky-600" />
                  <span>Admin & HR Quick Controls</span>
                </h3>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <Link
                  to="/employees"
                  className="p-4 bg-slate-50 hover:bg-sky-50 border border-slate-200 hover:border-sky-300 rounded-xl transition-all block group"
                >
                  <Users className="w-5 h-5 text-sky-600 mb-2 group-hover:scale-110 transition-transform" />
                  <h4 className="text-sm font-semibold text-slate-800">Manage Employees</h4>
                  <p className="text-xs text-slate-500 mt-1">Add staff, assign skills, process terminations</p>
                </Link>

                <Link
                  to="/compliance"
                  className="p-4 bg-slate-50 hover:bg-purple-50 border border-slate-200 hover:border-purple-300 rounded-xl transition-all block group"
                >
                  <Award className="w-5 h-5 text-purple-600 mb-2 group-hover:scale-110 transition-transform" />
                  <h4 className="text-sm font-semibold text-slate-800">Compliance Rules</h4>
                  <p className="text-xs text-slate-500 mt-1">Configure weekly hours & rest constraints</p>
                </Link>

                <Link
                  to="/leave"
                  className="p-4 bg-slate-50 hover:bg-amber-50 border border-slate-200 hover:border-amber-300 rounded-xl transition-all block group"
                >
                  <FileText className="w-5 h-5 text-amber-600 mb-2 group-hover:scale-110 transition-transform" />
                  <h4 className="text-sm font-semibold text-slate-800">Leave Approvals</h4>
                  <p className="text-xs text-slate-500 mt-1">Review pending leave requests & overlaps</p>
                </Link>
              </div>
            </div>
          )}

          {(isScheduler || isDeptHead) && (
            <div className="glass-card p-6 rounded-2xl space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
                  <Layers className="w-5 h-5 text-sky-600" />
                  <span>Scheduling Engine & Department Operations</span>
                </h3>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <Link
                  to="/schedules"
                  className="p-4 bg-slate-50 hover:bg-sky-50 border border-slate-200 hover:border-sky-300 rounded-xl transition-all block group"
                >
                  <Calendar className="w-5 h-5 text-sky-600 mb-2 group-hover:scale-110 transition-transform" />
                  <h4 className="text-sm font-semibold text-slate-800">Generate Shift Schedule</h4>
                  <p className="text-xs text-slate-500 mt-1">Run OR-Tools solver for department requirements</p>
                </Link>

                <Link
                  to="/staffing-requirements"
                  className="p-4 bg-slate-50 hover:bg-emerald-50 border border-slate-200 hover:border-emerald-300 rounded-xl transition-all block group"
                >
                  <Clock className="w-5 h-5 text-emerald-600 mb-2 group-hover:scale-110 transition-transform" />
                  <h4 className="text-sm font-semibold text-slate-800">Staffing Requirements</h4>
                  <p className="text-xs text-slate-500 mt-1">Define mandatory shift coverage & skill ratios</p>
                </Link>
              </div>
            </div>
          )}

          {isWorker && (
            <div className="glass-card p-6 rounded-2xl space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
                  <Clock className="w-5 h-5 text-sky-600" />
                  <span>My Upcoming Roster</span>
                </h3>
                <Link to="/my-schedule" className="text-xs font-semibold text-sky-600 hover:underline flex items-center gap-1">
                  Full Roster <ArrowRight className="w-3.5 h-3.5" />
                </Link>
              </div>

              {myNextShifts.length === 0 ? (
                <div className="p-8 text-center text-slate-500 bg-slate-50 rounded-xl border border-dashed border-slate-200">
                  <Calendar className="w-8 h-8 text-slate-400 mx-auto mb-2" />
                  <p className="text-sm font-medium">No published upcoming shifts found.</p>
                  <p className="text-xs text-slate-400 mt-1">Check back when your department head publishes the new schedule.</p>
                </div>
              ) : (
                <div className="space-y-3">
                  {myNextShifts.map((shift) => (
                    <div key={shift.id} className="p-4 bg-slate-50 border border-slate-200 rounded-xl flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-xl bg-sky-100 text-sky-700 flex items-center justify-center font-bold text-xs">
                          {shift.shiftTemplateName.substring(0, 3)}
                        </div>
                        <div>
                          <h4 className="text-sm font-bold text-slate-800">{shift.shiftTemplateName} Shift</h4>
                          <p className="text-xs text-slate-500">{shift.assignmentDate} • {shift.shiftStartTime} - {shift.shiftEndTime}</p>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        {shift.isOvertime && <span className="badge badge-warning">Overtime</span>}
                        <span className="badge badge-info">{shift.departmentName}</span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Right Column: Status Summary */}
        <div className="space-y-6">
          <div className="glass-card p-6 rounded-2xl space-y-4">
            <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
              <CheckCircle2 className="w-5 h-5 text-emerald-600" />
              <span>System & Engine Status</span>
            </h3>
            <div className="space-y-3 text-xs">
              <div className="p-3 bg-slate-50 rounded-xl border border-slate-200 flex items-center justify-between">
                <span className="text-slate-600 font-medium">CP-SAT Engine</span>
                <span className="badge badge-active">Active (OR-Tools v9.8)</span>
              </div>

              <div className="p-3 bg-slate-50 rounded-xl border border-slate-200 flex items-center justify-between">
                <span className="text-slate-600 font-medium">Max Weekly Hours Cap</span>
                <span className="font-semibold text-slate-800">40.0 hrs</span>
              </div>

              <div className="p-3 bg-slate-50 rounded-xl border border-slate-200 flex items-center justify-between">
                <span className="text-slate-600 font-medium">Minimum Rest Period</span>
                <span className="font-semibold text-slate-800">11.0 hrs</span>
              </div>

              <div className="p-3 bg-slate-50 rounded-xl border border-slate-200 flex items-center justify-between">
                <span className="text-slate-600 font-medium">Max Consecutive Shifts</span>
                <span className="font-semibold text-slate-800">5 Shifts</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
