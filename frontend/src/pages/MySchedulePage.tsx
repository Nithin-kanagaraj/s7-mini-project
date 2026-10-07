import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { Calendar, Clock, Building, RefreshCw, AlertCircle } from 'lucide-react';

interface PersonalAssignment {
  id: string;
  employeeId: string;
  employeeName: string;
  departmentId: string;
  departmentName: string;
  shiftTemplateId: string;
  shiftTemplateName: string;
  shiftStartTime: string;
  shiftEndTime: string;
  assignmentDate: string;
  status: string;
  isOvertime: boolean;
}

export const MySchedulePage: React.FC = () => {
  const { user } = useAuth();
  const [assignments, setAssignments] = useState<PersonalAssignment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchMySchedule();
  }, []);

  const fetchMySchedule = async () => {
    setLoading(true);
    setError(null);
    try {
      const token = localStorage.getItem('access_token');
      const res = await fetch('/api/schedules/my-assignments', {
        headers: { Authorization: `Bearer ${token}` },
      });

      if (res.ok) {
        setAssignments(await res.json());
      } else {
        setError('Failed to fetch personal roster assignments');
      }
    } catch (e) {
      setError('Network error fetching personal schedule');
    } finally {
      setLoading(false);
    }
  };

  // Group assignments by week
  const calculateTotalWeeklyHours = (): number => {
    return assignments.reduce((acc) => acc + 8.0, 0); // Standard 8h shifts
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <Calendar className="w-6 h-6 text-sky-600" />
            <span>My Personal Shift Roster</span>
          </h2>
          <p className="text-xs text-slate-500 font-medium">
            Published shifts assigned to {user?.fullName || user?.username}
          </p>
        </div>

        <div className="px-4 py-2 bg-sky-50 border border-sky-200 rounded-xl text-sky-900 text-xs font-bold flex items-center gap-2">
          <Clock className="w-4 h-4 text-sky-600" />
          <span>Total Scheduled: {calculateTotalWeeklyHours()} Hours</span>
        </div>
      </div>

      {error && (
        <div className="p-4 bg-rose-50 border border-rose-200 text-rose-800 rounded-xl text-sm flex items-center gap-2">
          <AlertCircle className="w-5 h-5 text-rose-600" />
          <span>{error}</span>
        </div>
      )}

      {loading ? (
        <div className="glass-card p-12 text-center text-slate-500 rounded-2xl flex items-center justify-center gap-2">
          <RefreshCw className="w-5 h-5 animate-spin text-sky-600" />
          <span className="text-sm font-semibold">Loading your shift roster...</span>
        </div>
      ) : assignments.length === 0 ? (
        <div className="glass-card p-12 text-center text-slate-500 rounded-2xl space-y-3">
          <Calendar className="w-12 h-12 text-slate-400 mx-auto" />
          <h3 className="text-base font-bold text-slate-800">No Published Shifts Found</h3>
          <p className="text-xs text-slate-500 max-w-md mx-auto">
            You do not have any published shifts assigned for the upcoming schedule window.
          </p>
        </div>
      ) : (
        <div className="glass-card p-6 rounded-2xl space-y-4">
          <div className="divide-y divide-slate-200">
            {assignments.map((item) => (
              <div key={item.id} className="py-4 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-sky-500 to-cyan-600 text-white flex flex-col items-center justify-center font-bold shadow-md shadow-sky-500/20">
                    <span className="text-xs font-semibold">{item.shiftTemplateName.substring(0, 3)}</span>
                  </div>

                  <div>
                    <h4 className="text-base font-bold text-slate-900">{item.assignmentDate}</h4>
                    <p className="text-xs text-slate-500 flex items-center gap-1.5 mt-0.5 font-medium">
                      <Clock className="w-3.5 h-3.5 text-slate-400" />
                      <span>{item.shiftTemplateName} Shift ({item.shiftStartTime} - {item.shiftEndTime})</span>
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  {item.isOvertime && (
                    <span className="badge badge-warning">Overtime Shift</span>
                  )}
                  <div className="flex items-center gap-1.5 px-3 py-1 bg-slate-100 rounded-lg text-xs font-semibold text-slate-700">
                    <Building className="w-3.5 h-3.5 text-slate-500" />
                    <span>{item.departmentName}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
