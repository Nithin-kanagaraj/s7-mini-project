import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { 
  Calendar, Play, CheckCircle2, AlertTriangle, AlertCircle, Clock, 
  UserCheck, Send, RefreshCw, X, User, ChevronRight, UserPlus
} from 'lucide-react';

interface Department {
  id: string;
  name: string;
}

interface Employee {
  id: string;
  fullName: string;
  departmentId: string;
  employmentStatus: string;
}

interface ShiftTemplate {
  id: string;
  name: string;
  startTime: string;
  endTime: string;
  durationHours: number;
}

interface ShortageAction {
  action: string;
  eligibleEmployees?: string[];
  candidateDepartments?: string[];
  note: string;
}

interface ShortageItemReport {
  department: string;
  date: string;
  shift: string;
  required: number;
  assigned: number;
  shortage: number;
  reason: string;
  detail: string;
  possibleActions: ShortageAction[];
}

interface ScheduleAssignment {
  id: string;
  employeeId?: string;
  employeeName?: string;
  departmentId: string;
  departmentName: string;
  shiftTemplateId: string;
  shiftTemplateName: string;
  shiftStartTime: string;
  shiftEndTime: string;
  assignmentDate: string;
  status: 'ASSIGNED' | 'UNFILLED' | 'CANCELLED';
  isOvertime: boolean;
  version: number;
}

interface Schedule {
  id: string;
  departmentId: string;
  departmentName: string;
  periodStart: string;
  periodEnd: string;
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
  generatedById: string;
  generatedByName: string;
  generatedAt: string;
  publishedAt?: string;
  hasShortages: boolean;
  solveStatus: 'OPTIMAL' | 'FEASIBLE' | 'TIMEOUT_PARTIAL' | 'INFEASIBLE';
  solveTimeMs: number;
  assignments: ScheduleAssignment[];
  shortageReport: ShortageItemReport[];
}

interface SanityWarning {
  date: string;
  shiftName: string;
  skillName: string;
  requiredCount: number;
  qualifiedStaffCount: number;
  warningMessage: string;
}

export const ScheduleManagementPage: React.FC = () => {
  const { isAdmin, isScheduler } = useAuth();

  const [departments, setDepartments] = useState<Department[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [shiftTemplates, setShiftTemplates] = useState<ShiftTemplate[]>([]);

  // Selected Query Params
  const [selectedDeptId, setSelectedDeptId] = useState<string>('');
  const [periodStart, setPeriodStart] = useState<string>(new Date().toISOString().split('T')[0]);
  const [periodDays, setPeriodDays] = useState<number>(7);
  const [archiveExisting, setArchiveExisting] = useState<boolean>(false);

  // Active Schedule
  const [currentSchedule, setCurrentSchedule] = useState<Schedule | null>(null);
  const [existingSchedules, setExistingSchedules] = useState<Schedule[]>([]);

  // Loaders & Errors
  const [loading, setLoading] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Pre-Check Sanity Warnings
  const [sanityWarnings, setSanityWarnings] = useState<SanityWarning[]>([]);
  const [isPreCheckOpen, setIsPreCheckOpen] = useState(false);

  // Manual Edit Modal
  const [editingAssignment, setEditingAssignment] = useState<ScheduleAssignment | null>(null);
  const [editEmployeeId, setEditEmployeeId] = useState<string>('');
  const [editStatus, setEditStatus] = useState<'ASSIGNED' | 'UNFILLED' | 'CANCELLED'>('ASSIGNED');
  const [editIsOvertime, setEditIsOvertime] = useState<boolean>(false);
  const [editError, setEditError] = useState<string | null>(null);

  // Publish Modal & Acknowledgment
  const [isPublishModalOpen, setIsPublishModalOpen] = useState(false);
  const [ackShortages, setAckShortages] = useState(false);
  const [publishing, setPublishing] = useState(false);

  // Roster View Mode
  const [viewMode, setViewMode] = useState<'matrix' | 'list' | 'shortages'>('matrix');

  useEffect(() => {
    fetchInitialData();
  }, []);

  const fetchInitialData = async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem('access_token');
      const headers = { Authorization: `Bearer ${token}` };

      const [deptRes, empRes, shiftRes, schedRes] = await Promise.all([
        fetch('/api/departments', { headers }),
        fetch('/api/employees?size=250', { headers }),
        fetch('/api/shift-templates', { headers }),
        fetch('/api/schedules', { headers }),
      ]);

      if (deptRes.ok) {
        const depts = await deptRes.json();
        setDepartments(depts.filter((d: any) => d.isActive));
        if (depts.length > 0 && !selectedDeptId) {
          setSelectedDeptId(depts[0].id);
        }
      }

      if (empRes.ok) {
        const empData = await empRes.json();
        setEmployees(empData.content || empData);
      }

      if (shiftRes.ok) {
        setShiftTemplates(await shiftRes.json());
      }

      if (schedRes.ok) {
        const scheds = await schedRes.json();
        setExistingSchedules(scheds);
        if (scheds.length > 0) {
          setCurrentSchedule(scheds[0]);
        }
      }
    } catch (e) {
      setError('Failed to initialize schedule builder components');
    } finally {
      setLoading(false);
    }
  };

  const calculatePeriodEnd = (startStr: string, days: number): string => {
    const d = new Date(startStr);
    d.setDate(d.getDate() + (days - 1));
    return d.toISOString().split('T')[0];
  };

  const handlePreCheck = async () => {
    if (!selectedDeptId) return;
    setLoading(true);
    setError(null);
    const periodEnd = calculatePeriodEnd(periodStart, periodDays);

    try {
      const token = localStorage.getItem('access_token');
      const res = await fetch(`/api/staffing-requirements/warnings?departmentId=${selectedDeptId}&startDate=${periodStart}&endDate=${periodEnd}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (res.ok) {
        const warnings = await res.json();
        setSanityWarnings(warnings);
        setIsPreCheckOpen(true);
      } else {
        setError('Failed to compute staffing pre-check sanity warnings');
      }
    } catch (e) {
      setError('Network error running pre-check sanity warnings');
    } finally {
      setLoading(false);
    }
  };

  const handleGenerateSchedule = async () => {
    if (!selectedDeptId) {
      setError('Please select a target department first');
      return;
    }

    setGenerating(true);
    setError(null);
    setSuccessMsg(null);
    const periodEnd = calculatePeriodEnd(periodStart, periodDays);

    try {
      const token = localStorage.getItem('access_token');
      const res = await fetch('/api/schedules/generate', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          departmentId: selectedDeptId,
          periodStart,
          periodEnd,
          archiveExisting,
        }),
      });

      if (res.status === 409) {
        const data = await res.json();
        setError(data.message || 'Schedule conflict exists or solve is already in progress');
        return;
      }

      if (!res.ok) {
        const data = await res.json().catch(() => ({ message: 'Schedule generation failed' }));
        setError(data.message || 'Failed to execute CP-SAT schedule solver');
        return;
      }

      const generated = await res.json();
      setCurrentSchedule(generated);
      setExistingSchedules((prev) => [generated, ...prev.filter((s) => s.id !== generated.id)]);
      setSuccessMsg(`Schedule solved successfully in ${generated.solveTimeMs} ms with status: ${generated.solveStatus}`);
    } catch (e) {
      setError('Network error during CP-SAT schedule generation');
    } finally {
      setGenerating(false);
    }
  };

  const handleOpenEditModal = (assignment: ScheduleAssignment) => {
    setEditingAssignment(assignment);
    setEditEmployeeId(assignment.employeeId || '');
    setEditStatus(assignment.status);
    setEditIsOvertime(assignment.isOvertime || false);
    setEditError(null);
  };

  const handleSaveManualEdit = async () => {
    if (!editingAssignment || !currentSchedule) return;

    setLoading(true);
    setEditError(null);

    try {
      const token = localStorage.getItem('access_token');
      const res = await fetch(`/api/schedules/${currentSchedule.id}/assignments/${editingAssignment.id}`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          employeeId: editEmployeeId || null,
          status: editStatus,
          isOvertime: editIsOvertime,
          version: editingAssignment.version,
        }),
      });

      if (!res.ok) {
        const errData = await res.json().catch(() => ({ message: 'Manual assignment update failed' }));
        // Surface EXACT named constraint failure message!
        setEditError(errData.message || 'Failed to update assignment');
        return;
      }

      await res.json();

      // Refresh schedule state
      const refreshedRes = await fetch(`/api/schedules/${currentSchedule.id}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (refreshedRes.ok) {
        const refreshed = await refreshedRes.json();
        setCurrentSchedule(refreshed);
      }

      setEditingAssignment(null);
      setSuccessMsg('Manual schedule edit revalidated and updated successfully');
    } catch (e) {
      setEditError('Network error updating schedule assignment');
    } finally {
      setLoading(false);
    }
  };

  const handleInlineAssignOvertime = async (assignment: ScheduleAssignment, empId: string) => {
    if (!currentSchedule) return;
    setLoading(true);
    setError(null);

    try {
      const token = localStorage.getItem('access_token');
      const res = await fetch(`/api/schedules/${currentSchedule.id}/assignments/${assignment.id}`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          employeeId: empId,
          status: 'ASSIGNED',
          isOvertime: true,
          version: assignment.version,
        }),
      });

      if (!res.ok) {
        const errData = await res.json().catch(() => ({ message: 'Inline overtime assignment failed' }));
        setError(errData.message || 'Inline assignment rejected by constraint checker');
        return;
      }

      const refreshedRes = await fetch(`/api/schedules/${currentSchedule.id}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (refreshedRes.ok) {
        setCurrentSchedule(await refreshedRes.json());
        setSuccessMsg('Inline overtime assignment applied successfully!');
      }
    } catch (e) {
      setError('Network error applying inline assignment');
    } finally {
      setLoading(false);
    }
  };

  const handlePublishSchedule = async () => {
    if (!currentSchedule) return;
    setPublishing(true);
    setError(null);

    try {
      const token = localStorage.getItem('access_token');
      const res = await fetch(`/api/schedules/${currentSchedule.id}/publish`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          acknowledgeShortages: ackShortages,
        }),
      });

      if (!res.ok) {
        const errData = await res.json().catch(() => ({ message: 'Failed to publish schedule' }));
        setError(errData.message || 'Publish rejected by server');
        setIsPublishModalOpen(false);
        return;
      }

      const published = await res.json();
      setCurrentSchedule(published);
      setIsPublishModalOpen(false);
      setSuccessMsg('Schedule published successfully to hospital roster!');
    } catch (e) {
      setError('Network error during schedule publish');
    } finally {
      setPublishing(false);
    }
  };

  const getDatesInRange = (startStr: string, endStr: string): string[] => {
    const dates: string[] = [];
    const current = new Date(startStr);
    const last = new Date(endStr);
    while (current <= last) {
      dates.push(current.toISOString().split('T')[0]);
      current.setDate(current.getDate() + 1);
    }
    return dates;
  };

  const activeDates = currentSchedule ? getDatesInRange(currentSchedule.periodStart, currentSchedule.periodEnd) : [];

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <Calendar className="w-6 h-6 text-sky-600" />
            <span>Schedule Engine & Roster Management</span>
          </h2>
          <p className="text-xs text-slate-500 font-medium">
            Google OR-Tools CP-SAT Automated Shift Optimization & Roster Editing
          </p>
        </div>

        {currentSchedule && (isAdmin || isScheduler) && currentSchedule.status === 'DRAFT' && (
          <button
            onClick={() => {
              setAckShortages(false);
              setIsPublishModalOpen(true);
            }}
            className="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-sm rounded-xl shadow-lg shadow-emerald-600/20 transition-all flex items-center gap-2 cursor-pointer"
          >
            <Send className="w-4 h-4" />
            <span>Publish Schedule</span>
          </button>
        )}
      </div>

      {/* Notifications */}
      {error && (
        <div className="p-4 bg-rose-50 border border-rose-200 text-rose-800 rounded-xl text-sm flex items-start gap-3">
          <AlertCircle className="w-5 h-5 text-rose-600 shrink-0 mt-0.5" />
          <div className="flex-1">{error}</div>
          <button onClick={() => setError(null)} className="text-rose-400 hover:text-rose-600">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {successMsg && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-sm flex items-start gap-3">
          <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" />
          <div className="flex-1">{successMsg}</div>
          <button onClick={() => setSuccessMsg(null)} className="text-emerald-400 hover:text-emerald-600">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Schedule Generator Control Panel */}
      {(isAdmin || isScheduler) && (
        <div className="glass-card p-6 rounded-2xl space-y-5">
          <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
            <Play className="w-5 h-5 text-sky-600" />
            <span>Schedule Generation Parameters</span>
          </h3>

          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                Target Department
              </label>
              <select
                value={selectedDeptId}
                onChange={(e) => setSelectedDeptId(e.target.value)}
                className="w-full px-3.5 py-2 bg-slate-50 border border-slate-300 rounded-xl text-sm font-medium focus:ring-2 focus:ring-sky-500 focus:outline-none"
              >
                {departments.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                Period Start Date
              </label>
              <input
                type="date"
                value={periodStart}
                onChange={(e) => setPeriodStart(e.target.value)}
                className="w-full px-3.5 py-2 bg-slate-50 border border-slate-300 rounded-xl text-sm font-medium focus:ring-2 focus:ring-sky-500 focus:outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                Window Duration
              </label>
              <select
                value={periodDays}
                onChange={(e) => setPeriodDays(Number(e.target.value))}
                className="w-full px-3.5 py-2 bg-slate-50 border border-slate-300 rounded-xl text-sm font-medium focus:ring-2 focus:ring-sky-500 focus:outline-none"
              >
                <option value={7}>7 Days (1 Week)</option>
                <option value={14}>14 Days (2 Weeks)</option>
              </select>
            </div>

            <div className="flex flex-col justify-end">
              <label className="inline-flex items-center gap-2 text-xs font-semibold text-slate-700 mb-2.5 cursor-pointer">
                <input
                  type="checkbox"
                  checked={archiveExisting}
                  onChange={(e) => setArchiveExisting(e.target.checked)}
                  className="rounded text-sky-600 focus:ring-sky-500"
                />
                <span>Archive overlapping schedules</span>
              </label>
            </div>
          </div>

          <div className="flex items-center gap-3 pt-2 border-t border-slate-200">
            <button
              onClick={handlePreCheck}
              disabled={loading || generating}
              className="px-4 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-xs rounded-xl transition-all flex items-center gap-2 cursor-pointer disabled:opacity-50"
            >
              <AlertTriangle className="w-4 h-4 text-amber-600" />
              <span>Run Staffing Pre-Check Warnings</span>
            </button>

            <button
              onClick={handleGenerateSchedule}
              disabled={generating || loading}
              className="px-6 py-2.5 bg-sky-600 hover:bg-sky-500 text-white font-semibold text-sm rounded-xl shadow-lg shadow-sky-600/20 transition-all flex items-center gap-2 cursor-pointer disabled:opacity-50"
            >
              {generating ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  <span>Running CP-SAT Solver (Max 30s)...</span>
                </>
              ) : (
                <>
                  <Play className="w-4 h-4" />
                  <span>Generate Schedule</span>
                </>
              )}
            </button>
          </div>
        </div>
      )}

      {/* Active Schedule Overview Status Bar */}
      {currentSchedule && (
        <div className="glass-card p-6 rounded-2xl space-y-4">
          <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 border-b border-slate-200 pb-4">
            <div>
              <div className="flex items-center gap-3">
                <h3 className="text-lg font-bold text-slate-900">
                  {currentSchedule.departmentName} Department Schedule
                </h3>
                <span className={`badge ${currentSchedule.status === 'PUBLISHED' ? 'badge-active' : 'badge-warning'}`}>
                  {currentSchedule.status}
                </span>
              </div>
              <p className="text-xs text-slate-500 mt-1">
                Period: <span className="font-semibold text-slate-700">{currentSchedule.periodStart}</span> to{' '}
                <span className="font-semibold text-slate-700">{currentSchedule.periodEnd}</span> • Generated by{' '}
                <span className="font-semibold text-slate-700">{currentSchedule.generatedByName}</span>
              </p>
            </div>

            <div className="flex items-center gap-3">
              {/* Distinct visual treatment for TIMEOUT_PARTIAL vs OPTIMAL / FEASIBLE */}
              {currentSchedule.solveStatus === 'TIMEOUT_PARTIAL' ? (
                <div className="px-3.5 py-1.5 rounded-xl bg-purple-100 border border-purple-300 text-purple-900 text-xs font-bold flex items-center gap-2">
                  <Clock className="w-4 h-4 text-purple-600 animate-pulse" />
                  <span>TIMEOUT_PARTIAL (30s Limit Reached) — {currentSchedule.solveTimeMs} ms</span>
                </div>
              ) : currentSchedule.solveStatus === 'OPTIMAL' ? (
                <div className="px-3.5 py-1.5 rounded-xl bg-emerald-100 border border-emerald-300 text-emerald-900 text-xs font-bold flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                  <span>OPTIMAL SOLVED — {currentSchedule.solveTimeMs} ms</span>
                </div>
              ) : (
                <div className="px-3.5 py-1.5 rounded-xl bg-sky-100 border border-sky-300 text-sky-900 text-xs font-bold flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-sky-600" />
                  <span>{currentSchedule.solveStatus} — {currentSchedule.solveTimeMs} ms</span>
                </div>
              )}

              {currentSchedule.hasShortages ? (
                <div className="px-3 py-1.5 rounded-xl bg-rose-100 border border-rose-300 text-rose-800 text-xs font-bold flex items-center gap-1.5">
                  <AlertTriangle className="w-4 h-4 text-rose-600" />
                  <span>{currentSchedule.shortageReport.length} Unfilled Shortages</span>
                </div>
              ) : (
                <div className="px-3 py-1.5 rounded-xl bg-emerald-100 border border-emerald-300 text-emerald-800 text-xs font-bold flex items-center gap-1.5">
                  <UserCheck className="w-4 h-4 text-emerald-600" />
                  <span>100% Fully Staffed</span>
                </div>
              )}
            </div>
          </div>

          {/* Schedule Navigation & Tabs */}
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 pt-1">
            <div className="flex gap-2">
              <button
                onClick={() => setViewMode('matrix')}
                className={`px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                  viewMode === 'matrix' ? 'bg-sky-600 text-white shadow-md' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                }`}
              >
                Roster Matrix View
              </button>
              <button
                onClick={() => setViewMode('shortages')}
                className={`px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 ${
                  viewMode === 'shortages' ? 'bg-sky-600 text-white shadow-md' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                }`}
              >
                <AlertTriangle className="w-3.5 h-3.5" />
                <span>Shortage Diagnostic ({currentSchedule.shortageReport?.length || 0})</span>
              </button>
            </div>

            {/* Existing Schedules Selector */}
            {existingSchedules.length > 1 && (
              <div className="flex items-center gap-2">
                <span className="text-xs font-semibold text-slate-500">History:</span>
                <select
                  value={currentSchedule.id}
                  onChange={(e) => {
                    const found = existingSchedules.find((s) => s.id === e.target.value);
                    if (found) setCurrentSchedule(found);
                  }}
                  className="px-3 py-1.5 bg-slate-50 border border-slate-300 rounded-xl text-xs font-medium focus:ring-2 focus:ring-sky-500"
                >
                  {existingSchedules.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.departmentName} ({s.periodStart} ~ {s.periodEnd}) - {s.status}
                    </option>
                  ))}
                </select>
              </div>
            )}
          </div>
        </div>
      )}

      {/* View Mode 1: Shortage Diagnostic Report with Inline Actionable Suggestions */}
      {currentSchedule && viewMode === 'shortages' && (
        <div className="glass-card p-6 rounded-2xl space-y-6">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-lg font-bold text-slate-900 flex items-center gap-2">
                <AlertTriangle className="w-5 h-5 text-rose-600" />
                <span>Shortage Diagnostics & Resolution Engine</span>
              </h3>
              <p className="text-xs text-slate-500 mt-0.5">
                Structured root-cause diagnosis for unfilled staffing requirements
              </p>
            </div>
          </div>

          {currentSchedule.shortageReport.length === 0 ? (
            <div className="p-8 text-center text-emerald-700 bg-emerald-50 rounded-2xl border border-emerald-200">
              <CheckCircle2 className="w-10 h-10 text-emerald-600 mx-auto mb-2" />
              <h4 className="text-base font-bold">No Shortages Detected!</h4>
              <p className="text-xs text-emerald-600 mt-1">
                The CP-SAT solver successfully assigned qualified staff to every required shift.
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              {currentSchedule.shortageReport.map((report, idx) => (
                <div key={idx} className="p-5 bg-rose-50/70 border border-rose-200 rounded-2xl space-y-4">
                  <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-bold text-slate-900">{report.date}</span>
                        <span className="badge badge-purple">{report.shift} Shift</span>
                        <span className="badge badge-danger">Shortage: {report.shortage} Staff</span>
                      </div>
                      <p className="text-xs text-slate-600 font-medium mt-1">
                        Requirement: {report.required} Needed, {report.assigned} Assigned
                      </p>
                    </div>

                    <div className="px-3 py-1 rounded-lg bg-rose-100 text-rose-900 text-xs font-bold uppercase tracking-wider">
                      Reason: {report.reason}
                    </div>
                  </div>

                  <div className="text-xs text-slate-700 bg-white p-3 rounded-xl border border-rose-200">
                    <span className="font-bold text-slate-900">Diagnostic Details: </span>
                    {report.detail}
                  </div>

                  {/* Inline Action Suggestions */}
                  <div className="space-y-2 pt-2 border-t border-rose-200">
                    <h5 className="text-xs font-bold text-slate-800 uppercase tracking-wider flex items-center gap-1.5">
                      <UserPlus className="w-3.5 h-3.5 text-sky-600" />
                      <span>Possible Actions & Inline Assignments</span>
                    </h5>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                      {report.possibleActions.map((action, aIdx) => (
                        <div key={aIdx} className="p-3 bg-white rounded-xl border border-slate-200 space-y-2 text-xs">
                          <div className="font-bold text-slate-800 flex items-center gap-1">
                            <ChevronRight className="w-3.5 h-3.5 text-sky-500" />
                            <span>{action.action}</span>
                          </div>
                          <p className="text-slate-600 text-[11px]">{action.note}</p>

                          {/* Inline Overtime Candidate Quick-Buttons */}
                          {action.action === 'ASSIGN_OVERTIME' && action.eligibleEmployees && action.eligibleEmployees.length > 0 && (
                            <div className="pt-2 flex flex-wrap items-center gap-2">
                              <span className="text-[11px] font-bold text-slate-500">Assign Eligible Candidate:</span>
                              {action.eligibleEmployees.map((empId) => {
                                const emp = employees.find((e) => e.id === empId);
                                const empName = emp ? emp.fullName : empId.substring(0, 8);
                                // Find corresponding unfilled assignment row
                                const unfilledSa = currentSchedule.assignments.find(
                                  (sa) => sa.assignmentDate === report.date && sa.shiftTemplateName === report.shift && sa.status === 'UNFILLED'
                                );

                                return (
                                  <button
                                    key={empId}
                                    onClick={() => unfilledSa && handleInlineAssignOvertime(unfilledSa, empId)}
                                    className="px-2.5 py-1 bg-sky-100 hover:bg-sky-200 text-sky-800 font-bold text-[11px] rounded-lg transition-colors flex items-center gap-1 cursor-pointer"
                                  >
                                    <User className="w-3 h-3" />
                                    <span>Assign {empName} (+Overtime)</span>
                                  </button>
                                );
                              })}
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* View Mode 2: Roster Matrix View */}
      {currentSchedule && viewMode === 'matrix' && (
        <div className="glass-card p-6 rounded-2xl space-y-6">
          <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
            <div>
              <h3 className="text-lg font-bold text-slate-900">Hospital Roster Grid</h3>
              <p className="text-xs text-slate-500">
                Click any assignment block to manually revalidate or assign staff.
              </p>
            </div>
          </div>

          <div className="overflow-x-auto border border-slate-200 rounded-xl">
            <table className="w-full text-left border-collapse min-w-[800px]">
              <thead>
                <tr className="bg-slate-100 text-slate-700 text-xs font-bold uppercase tracking-wider border-b border-slate-200">
                  <th className="p-3 w-44">Date / Shift</th>
                  {shiftTemplates.map((s) => (
                    <th key={s.id} className="p-3 text-center border-l border-slate-200">
                      {s.name} ({s.startTime.substring(0, 5)} - {s.endTime.substring(0, 5)})
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 text-xs">
                {activeDates.map((d) => (
                  <tr key={d} className="hover:bg-slate-50/80">
                    <td className="p-3 font-bold text-slate-800 bg-slate-50">
                      {d}
                    </td>

                    {shiftTemplates.map((shift) => {
                      const matchingAssignments = currentSchedule.assignments.filter(
                        (sa) => sa.assignmentDate === d && sa.shiftTemplateId === shift.id
                      );

                      return (
                        <td key={shift.id} className="p-2 border-l border-slate-200 vertical-top">
                          {matchingAssignments.length === 0 ? (
                            <span className="text-slate-400 italic text-[11px]">No requirement</span>
                          ) : (
                            <div className="space-y-1.5">
                              {matchingAssignments.map((sa) => (
                                <div
                                  key={sa.id}
                                  onClick={() => handleOpenEditModal(sa)}
                                  className={`p-2 rounded-xl border text-left transition-all cursor-pointer hover:scale-[1.02] ${
                                    sa.status === 'UNFILLED'
                                      ? 'bg-rose-50 border-rose-300 text-rose-900 shadow-xs ring-1 ring-rose-400/50'
                                      : sa.isOvertime
                                      ? 'bg-amber-50 border-amber-300 text-amber-900'
                                      : 'bg-white border-slate-200 hover:border-sky-400 text-slate-800 shadow-xs'
                                  }`}
                                >
                                  {sa.status === 'UNFILLED' ? (
                                    <div className="flex items-center justify-between gap-1">
                                      <span className="font-bold text-rose-700 text-[11px] flex items-center gap-1">
                                        <AlertTriangle className="w-3 h-3 text-rose-600" />
                                        <span>UNFILLED SLOT</span>
                                      </span>
                                      <span className="text-[10px] font-bold text-rose-600 underline">Assign</span>
                                    </div>
                                  ) : (
                                    <div>
                                      <div className="font-bold text-slate-900 text-xs flex items-center justify-between">
                                        <span>{sa.employeeName || 'Assigned Staff'}</span>
                                        {sa.isOvertime && (
                                          <span className="text-[9px] font-bold bg-amber-200 text-amber-900 px-1.5 py-0.5 rounded">
                                            OT
                                          </span>
                                        )}
                                      </div>
                                    </div>
                                  )}
                                </div>
                              ))}
                            </div>
                          )}
                        </td>
                      );
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Modal 1: Pre-Check Sanity Warnings */}
      {isPreCheckOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-2xl w-full p-6 space-y-5 shadow-2xl">
            <div className="flex justify-between items-center border-b border-slate-200 pb-3">
              <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <AlertTriangle className="w-5 h-5 text-amber-600" />
                <span>Staffing Pre-Check Warnings</span>
              </h3>
              <button onClick={() => setIsPreCheckOpen(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            {sanityWarnings.length === 0 ? (
              <div className="p-6 text-center text-emerald-700 bg-emerald-50 rounded-xl">
                <CheckCircle2 className="w-8 h-8 text-emerald-600 mx-auto mb-2" />
                <p className="font-bold text-sm">All staffing requirements have qualified active staff!</p>
              </div>
            ) : (
              <div className="space-y-3 max-h-96 overflow-y-auto pr-1">
                {sanityWarnings.map((w, idx) => (
                  <div key={idx} className="p-4 bg-amber-50 border border-amber-200 rounded-xl space-y-1 text-xs text-amber-900">
                    <div className="font-bold text-amber-950 flex items-center justify-between">
                      <span>{w.date} • {w.shiftName} Shift</span>
                      <span className="badge badge-warning">{w.skillName} Required</span>
                    </div>
                    <p>{w.warningMessage}</p>
                  </div>
                ))}
              </div>
            )}

            <div className="flex justify-end">
              <button
                onClick={() => setIsPreCheckOpen(false)}
                className="px-4 py-2 bg-slate-200 hover:bg-slate-300 text-slate-800 font-semibold text-xs rounded-xl"
              >
                Close Warnings
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal 2: Manual Edit Assignment with Named Constraint Rejection Display */}
      {editingAssignment && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 space-y-5 shadow-2xl">
            <div className="flex justify-between items-center border-b border-slate-200 pb-3">
              <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <UserCheck className="w-5 h-5 text-sky-600" />
                <span>Manual Schedule Edit</span>
              </h3>
              <button onClick={() => setEditingAssignment(null)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Error Banner: Surfaces exact named constraint violation! */}
            {editError && (
              <div className="p-3.5 bg-rose-50 border border-rose-200 text-rose-900 rounded-xl text-xs font-semibold flex items-start gap-2">
                <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                <span>{editError}</span>
              </div>
            )}

            <div className="space-y-4 text-xs">
              <div className="p-3 bg-slate-50 rounded-xl border border-slate-200 space-y-1">
                <div>Date: <span className="font-bold text-slate-900">{editingAssignment.assignmentDate}</span></div>
                <div>Shift: <span className="font-bold text-slate-900">{editingAssignment.shiftTemplateName}</span></div>
              </div>

              <div>
                <label className="block font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                  Assign Active Employee
                </label>
                <select
                  value={editEmployeeId}
                  onChange={(e) => setEditEmployeeId(e.target.value)}
                  className="w-full px-3.5 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs font-medium focus:ring-2 focus:ring-sky-500"
                >
                  <option value="">-- Unfilled Slot --</option>
                  {employees
                    .filter((e) => e.employmentStatus === 'ACTIVE')
                    .map((e) => (
                      <option key={e.id} value={e.id}>
                        {e.fullName} ({e.id.substring(0, 8)})
                      </option>
                    ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                    Assignment Status
                  </label>
                  <select
                    value={editStatus}
                    onChange={(e) => setEditStatus(e.target.value as any)}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs font-medium"
                  >
                    <option value="ASSIGNED">ASSIGNED</option>
                    <option value="UNFILLED">UNFILLED</option>
                    <option value="CANCELLED">CANCELLED</option>
                  </select>
                </div>

                <div className="flex items-end pb-2">
                  <label className="inline-flex items-center gap-2 font-bold text-slate-700 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={editIsOvertime}
                      onChange={(e) => setEditIsOvertime(e.target.checked)}
                      className="rounded text-sky-600"
                    />
                    <span>Is Overtime</span>
                  </label>
                </div>
              </div>
            </div>

            <div className="flex justify-end gap-3 pt-3 border-t border-slate-200">
              <button
                onClick={() => setEditingAssignment(null)}
                className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-xs rounded-xl"
              >
                Cancel
              </button>
              <button
                onClick={handleSaveManualEdit}
                disabled={loading}
                className="px-5 py-2 bg-sky-600 hover:bg-sky-500 text-white font-semibold text-xs rounded-xl shadow-md disabled:opacity-50"
              >
                Save & Revalidate
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal 3: Publish Workflow with Explicit Shortage Acknowledgment Checkbox */}
      {isPublishModalOpen && currentSchedule && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 space-y-5 shadow-2xl">
            <div className="flex justify-between items-center border-b border-slate-200 pb-3">
              <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <Send className="w-5 h-5 text-emerald-600" />
                <span>Publish Schedule</span>
              </h3>
              <button onClick={() => setIsPublishModalOpen(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <p className="text-xs text-slate-600 leading-relaxed">
              Publishing this schedule will lock the draft and make shift assignments visible to all assigned healthcare workers.
            </p>

            {/* Shortage Acknowledgment Checkbox */}
            {currentSchedule.hasShortages && (
              <div className="p-4 bg-rose-50 border border-rose-300 rounded-xl space-y-3">
                <div className="flex items-start gap-2 text-rose-900 font-bold text-xs">
                  <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                  <span>Schedule contains {currentSchedule.shortageReport.length} unfilled shortages!</span>
                </div>
                <p className="text-[11px] text-rose-800">
                  Explicit acknowledgment is required to proceed with publishing an understaffed schedule.
                </p>

                <label className="inline-flex items-center gap-2 text-xs font-bold text-rose-950 cursor-pointer pt-1">
                  <input
                    type="checkbox"
                    checked={ackShortages}
                    onChange={(e) => setAckShortages(e.target.checked)}
                    className="rounded text-rose-600 focus:ring-rose-500"
                  />
                  <span>I acknowledge these shortages and want to publish anyway</span>
                </label>
              </div>
            )}

            <div className="flex justify-end gap-3 pt-2 border-t border-slate-200">
              <button
                onClick={() => setIsPublishModalOpen(false)}
                className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-xs rounded-xl"
              >
                Cancel
              </button>
              <button
                onClick={handlePublishSchedule}
                disabled={publishing || (currentSchedule.hasShortages && !ackShortages)}
                className="px-5 py-2 bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs rounded-xl shadow-md disabled:opacity-40 disabled:cursor-not-allowed"
              >
                {publishing ? 'Publishing...' : 'Confirm & Publish'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
