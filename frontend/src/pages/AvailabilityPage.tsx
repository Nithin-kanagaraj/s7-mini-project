import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { Calendar, Plus, Trash2, AlertCircle, X, CheckCircle2 } from 'lucide-react';

interface ShiftTemplate {
  id: string;
  name: string;
}

interface Availability {
  id: string;
  employeeId: string;
  employeeName: string;
  unavailableDate: string;
  shiftTemplateId?: string;
  shiftTemplateName: string;
  reason?: string;
}

export const AvailabilityPage: React.FC = () => {
  const { user } = useAuth();
  const [availabilityList, setAvailabilityList] = useState<Availability[]>([]);
  const [shiftTemplates, setShiftTemplates] = useState<ShiftTemplate[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [unavailableDate, setUnavailableDate] = useState(new Date().toISOString().split('T')[0]);
  const [shiftTemplateId, setShiftTemplateId] = useState('');
  const [reason, setReason] = useState('');

  const fetchShiftTemplates = async () => {
    try {
      const res = await fetch('/api/shift-templates', {
        headers: { Authorization: `Bearer ${localStorage.getItem('access_token')}` },
      });
      if (res.ok) {
        setShiftTemplates(await res.json());
      }
    } catch (e) {
      console.error(e);
    }
  };

  const fetchMyAvailability = async () => {
    if (!user?.employeeId) return;
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(`/api/availability/employee/${user.employeeId}`, {
        headers: { Authorization: `Bearer ${localStorage.getItem('access_token')}` },
      });

      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || 'Failed to fetch availability');
      }

      setAvailabilityList(await res.json());
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchShiftTemplates();
    fetchMyAvailability();
  }, [user]);

  const handleOpenCreate = () => {
    setUnavailableDate(new Date().toISOString().split('T')[0]);
    setShiftTemplateId('');
    setReason('');
    setIsModalOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    // Frontend validation for 6-month limit
    const maxAllowedDate = new Date();
    maxAllowedDate.setMonth(maxAllowedDate.getMonth() + 6);
    const selectedDateObj = new Date(unavailableDate);

    if (selectedDateObj > maxAllowedDate) {
      setError(`Cannot enter availability more than 6 months in advance (maximum date allowed: ${maxAllowedDate.toISOString().split('T')[0]})`);
      return;
    }

    try {
      const res = await fetch('/api/availability/my-availability', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${localStorage.getItem('access_token')}`,
        },
        body: JSON.stringify({
          unavailableDate,
          shiftTemplateId: shiftTemplateId || null,
          reason,
        }),
      });

      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || 'Failed to mark unavailability');
      }

      setIsModalOpen(false);
      fetchMyAvailability();
    } catch (err: any) {
      setError(err.message);
    }
  };

  const handleDelete = async (id: string) => {
    try {
      const res = await fetch(`/api/availability/my-availability/${id}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${localStorage.getItem('access_token')}` },
      });

      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || 'Failed to delete availability entry');
      }

      fetchMyAvailability();
    } catch (err: any) {
      setError(err.message);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">My Unavailability Exceptions</h1>
          <p className="text-sm text-slate-500">Mark specific dates or shifts when you are unavailable to work (up to 6 months in advance)</p>
        </div>
        <button
          onClick={handleOpenCreate}
          className="flex items-center gap-2 px-4 py-2.5 bg-sky-600 hover:bg-sky-700 text-white font-semibold rounded-xl shadow-md shadow-sky-600/20 transition-all cursor-pointer"
        >
          <Plus className="w-4 h-4" />
          <span>Mark Unavailable Date</span>
        </button>
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-sm flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-5 h-5 shrink-0" />
            <span>{error}</span>
          </div>
          <button onClick={() => setError(null)} className="text-rose-500 hover:text-rose-700">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Info Card */}
      <div className="p-4 rounded-2xl bg-sky-50 border border-sky-200 flex items-start gap-3 text-sky-900 text-xs">
        <CheckCircle2 className="w-5 h-5 text-sky-600 shrink-0 mt-0.5" />
        <div>
          <span className="font-bold">Exception-based Availability Model:</span> You are assumed to be available for scheduling by default. Only enter dates or specific shifts here when you have a conflict or exception.
        </div>
      </div>

      {/* Availability List */}
      <div className="glass-card rounded-2xl overflow-hidden">
        {loading ? (
          <div className="p-12 text-center text-slate-500">Loading availability records...</div>
        ) : availabilityList.length === 0 ? (
          <div className="p-12 text-center text-slate-500">
            <Calendar className="w-12 h-12 text-slate-300 mx-auto mb-3" />
            <p className="font-semibold text-slate-700">No unavailability exceptions recorded</p>
            <p className="text-xs text-slate-400 mt-1">You are fully available for all upcoming shifts.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200 text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  <th className="p-4">Unavailable Date</th>
                  <th className="p-4">Shift Scope</th>
                  <th className="p-4">Reason / Notes</th>
                  <th className="p-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {availabilityList.map((a) => (
                  <tr key={a.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="p-4 font-bold text-slate-900">{a.unavailableDate}</td>
                    <td className="p-4">
                      <span className="badge badge-warning">{a.shiftTemplateName}</span>
                    </td>
                    <td className="p-4 text-slate-600">{a.reason || 'No reason specified'}</td>
                    <td className="p-4 text-right">
                      <button
                        onClick={() => handleDelete(a.id)}
                        className="px-2.5 py-1.5 bg-rose-50 hover:bg-rose-100 text-rose-700 font-semibold rounded-lg text-xs flex items-center gap-1 ml-auto cursor-pointer"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                        <span>Remove</span>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-xs">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-100">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-bold text-slate-900">Mark Unavailability</h3>
              <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Unavailable Date</label>
                <input
                  type="date"
                  required
                  value={unavailableDate}
                  onChange={(e) => setUnavailableDate(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                />
                <p className="text-[11px] text-slate-400 mt-1">Must be within 6 months from today.</p>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Specific Shift (Optional)</label>
                <select
                  value={shiftTemplateId}
                  onChange={(e) => setShiftTemplateId(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                >
                  <option value="">All Shifts (Entire Day)</option>
                  {shiftTemplates.map((s) => (
                    <option key={s.id} value={s.id}>{s.name} Shift</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Reason / Notes</label>
                <input
                  type="text"
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  placeholder="e.g. Doctor appointment, family event"
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-3">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 text-sm font-medium text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-xl cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 text-sm font-semibold text-white bg-sky-600 hover:bg-sky-700 rounded-xl shadow-md cursor-pointer"
                >
                  Save Exception
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
