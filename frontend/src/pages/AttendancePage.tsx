import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { Clock, AlertCircle, CheckCircle2, RefreshCw, Plus, X, Info } from 'lucide-react';

interface AttendanceRecord {
  id: string;
  assignmentId: string;
  employeeId: string;
  employeeName: string;
  shiftTemplateName: string;
  assignmentDate: string;
  clockIn: string;
  clockOut: string;
  actualHours: number;
  plannedHours: number;
  entryMethod: string;
  recordedByUsername: string;
}

interface ScheduleAssignment {
  id: string;
  employeeId?: string;
  employeeName?: string;
  departmentName: string;
  shiftTemplateName: string;
  assignmentDate: string;
  status: string;
  isOvertime: boolean;
}

export const AttendancePage: React.FC = () => {
  const { isAdmin, isHr, isDeptHead } = useAuth();

  const [records, setRecords] = useState<AttendanceRecord[]>([]);
  const [assignments, setAssignments] = useState<ScheduleAssignment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Clock Entry Modal State
  const [selectedAssignmentId, setSelectedAssignmentId] = useState<string>('');
  const [clockInTime, setClockInTime] = useState<string>('');
  const [clockOutTime, setClockOutTime] = useState<string>('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
      const token = localStorage.getItem('access_token');
      const headers = { Authorization: `Bearer ${token}` };

      const [recRes, schedRes] = await Promise.all([
        fetch('/api/attendance', { headers }),
        fetch('/api/schedules', { headers }),
      ]);

      if (recRes.ok) {
        setRecords(await recRes.json());
      }

      if (schedRes.ok) {
        const scheds = await schedRes.json();
        const allAssigned: ScheduleAssignment[] = [];
        scheds.forEach((s: any) => {
          if (s.assignments) {
            s.assignments.forEach((a: any) => {
              if (a.status === 'ASSIGNED' && a.employeeId) {
                allAssigned.push(a);
              }
            });
          }
        });
        setAssignments(allAssigned);
      }
    } catch (e) {
      setError('Failed to load attendance records');
    } finally {
      setLoading(false);
    }
  };

  const handleRecordAttendance = async () => {
    if (!selectedAssignmentId || !clockInTime || !clockOutTime) {
      setError('Please fill in all clock-in and clock-out fields');
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      const token = localStorage.getItem('access_token');
      const targetAssignment = assignments.find((a) => a.id === selectedAssignmentId);
      const dateStr = targetAssignment ? targetAssignment.assignmentDate : new Date().toISOString().split('T')[0];

      const res = await fetch(`/api/attendance/${selectedAssignmentId}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          clockIn: `${dateStr}T${clockInTime}:00`,
          clockOut: `${dateStr}T${clockOutTime}:00`,
        }),
      });

      if (res.ok) {
        setSuccessMsg('Attendance clock-in/out recorded successfully!');
        setIsModalOpen(false);
        fetchData();
      } else {
        const err = await res.json().catch(() => ({ message: 'Failed to record attendance' }));
        setError(err.message || 'Failed to record attendance');
      }
    } catch (e) {
      setError('Network error recording attendance');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <Clock className="w-6 h-6 text-sky-600" />
            <span>Attendance & Actual Worked Hours</span>
          </h2>
          <p className="text-xs text-slate-500 font-medium">
            Manual clock-in/out logging and planned vs. actual worked hours tracking
          </p>
        </div>

        {(isAdmin || isHr || isDeptHead) && (
          <button
            onClick={() => setIsModalOpen(true)}
            className="px-4 py-2.5 bg-sky-600 hover:bg-sky-500 text-white font-semibold text-xs rounded-xl shadow-lg shadow-sky-600/20 transition-all flex items-center gap-2 cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>Record Clock Entry</span>
          </button>
        )}
      </div>

      {/* Copy Disclaimer Note */}
      <div className="p-4 bg-slate-100 border border-slate-200 rounded-2xl text-xs text-slate-600 flex items-start gap-3">
        <Info className="w-5 h-5 text-sky-600 shrink-0 mt-0.5" />
        <div>
          <span className="font-bold text-slate-800">Administrative Notice & Copy Disclaimer: </span>
          Compliance rules and work-hour targets in this system are hospital-configured administrative settings used for shift allocation and schedule optimization. They do not constitute a legal or contractual guarantee of labor law compliance.
        </div>
      </div>

      {error && (
        <div className="p-4 bg-rose-50 border border-rose-200 text-rose-800 rounded-xl text-sm flex items-center gap-2">
          <AlertCircle className="w-5 h-5 text-rose-600" />
          <span>{error}</span>
        </div>
      )}

      {successMsg && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-sm flex items-center gap-2">
          <CheckCircle2 className="w-5 h-5 text-emerald-600" />
          <span>{successMsg}</span>
        </div>
      )}

      {loading ? (
        <div className="glass-card p-12 text-center text-slate-500 rounded-2xl flex items-center justify-center gap-2">
          <RefreshCw className="w-5 h-5 animate-spin text-sky-600" />
          <span className="text-sm font-semibold">Loading attendance logs...</span>
        </div>
      ) : records.length === 0 ? (
        <div className="glass-card p-12 text-center text-slate-500 rounded-2xl space-y-3">
          <Clock className="w-12 h-12 text-slate-400 mx-auto" />
          <h3 className="text-base font-bold text-slate-800">No Attendance Records Recorded</h3>
          <p className="text-xs text-slate-500 max-w-md mx-auto">
            Clock-in/out timestamps recorded by authorized managers will appear here.
          </p>
        </div>
      ) : (
        <div className="glass-card p-6 rounded-2xl space-y-4">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="bg-slate-100 text-slate-700 font-bold uppercase tracking-wider border-b border-slate-200">
                  <th className="p-3">Employee</th>
                  <th className="p-3">Shift & Date</th>
                  <th className="p-3">Clock In</th>
                  <th className="p-3">Clock Out</th>
                  <th className="p-3">Planned vs. Actual</th>
                  <th className="p-3">Logged By</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200">
                {records.map((r) => (
                  <tr key={r.id} className="hover:bg-slate-50/80">
                    <td className="p-3 font-bold text-slate-900">{r.employeeName || 'Staff Member'}</td>
                    <td className="p-3">
                      <div className="font-semibold text-slate-800">{r.shiftTemplateName}</div>
                      <div className="text-[11px] text-slate-500">{r.assignmentDate}</div>
                    </td>
                    <td className="p-3 text-slate-700 font-mono">
                      {r.clockIn ? new Date(r.clockIn).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'N/A'}
                    </td>
                    <td className="p-3 text-slate-700 font-mono">
                      {r.clockOut ? new Date(r.clockOut).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'N/A'}
                    </td>
                    <td className="p-3">
                      <div className="flex items-center gap-2">
                        <span className="badge badge-info">{r.plannedHours ?? 8.0}h Planned</span>
                        <span className="badge badge-purple">{r.actualHours ? `${r.actualHours}h Actual` : 'Pending'}</span>
                      </div>
                    </td>
                    <td className="p-3 text-slate-600 font-medium">
                      {r.recordedByUsername || 'Admin'} ({r.entryMethod})
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Clock Entry Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 space-y-5 shadow-2xl">
            <div className="flex justify-between items-center border-b border-slate-200 pb-3">
              <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <Clock className="w-5 h-5 text-sky-600" />
                <span>Manual Attendance Entry</span>
              </h3>
              <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-4 text-xs">
              <div>
                <label className="block font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                  Select Shift Assignment
                </label>
                <select
                  value={selectedAssignmentId}
                  onChange={(e) => setSelectedAssignmentId(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs font-medium"
                >
                  <option value="">-- Choose Assigned Shift --</option>
                  {assignments.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.employeeName} - {a.shiftTemplateName} ({a.assignmentDate})
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                    Clock In Time
                  </label>
                  <input
                    type="time"
                    value={clockInTime}
                    onChange={(e) => setClockInTime(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs font-medium"
                  />
                </div>

                <div>
                  <label className="block font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                    Clock Out Time
                  </label>
                  <input
                    type="time"
                    value={clockOutTime}
                    onChange={(e) => setClockOutTime(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs font-medium"
                  />
                </div>
              </div>
            </div>

            <div className="flex justify-end gap-3 pt-3 border-t border-slate-200">
              <button
                onClick={() => setIsModalOpen(false)}
                className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-xs rounded-xl"
              >
                Cancel
              </button>
              <button
                onClick={handleRecordAttendance}
                disabled={submitting}
                className="px-5 py-2 bg-sky-600 hover:bg-sky-500 text-white font-semibold text-xs rounded-xl shadow-md disabled:opacity-50"
              >
                {submitting ? 'Recording...' : 'Save Attendance'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
