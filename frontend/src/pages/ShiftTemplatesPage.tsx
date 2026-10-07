import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { Plus, Moon, Sun, Sunset, AlertCircle, X } from 'lucide-react';

interface ShiftTemplate {
  id: string;
  name: string;
  startTime: string;
  endTime: string;
  durationHours: number;
  isOvernight: boolean;
}

export const ShiftTemplatesPage: React.FC = () => {
  const { isAdmin, isScheduler } = useAuth();
  const [shifts, setShifts] = useState<ShiftTemplate[]>([]);
  const [error, setError] = useState<string | null>(null);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingShift, setEditingShift] = useState<ShiftTemplate | null>(null);
  const [name, setName] = useState('');
  const [startTime, setStartTime] = useState('07:00');
  const [endTime, setEndTime] = useState('15:00');
  const [durationHours, setDurationHours] = useState('8.00');

  const fetchShifts = async () => {
    try {
      const res = await fetch('/api/shift-templates', {
        headers: { Authorization: `Bearer ${localStorage.getItem('access_token')}` },
      });
      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || 'Failed to fetch shift templates');
      }
      const data = await res.json();
      setShifts(data);
    } catch (err: any) {
      setError(err.message);
    }
  };

  useEffect(() => {
    fetchShifts();
  }, []);

  const handleOpenCreate = () => {
    setEditingShift(null);
    setName('');
    setStartTime('07:00');
    setEndTime('15:00');
    setDurationHours('8.00');
    setIsModalOpen(true);
  };

  const handleOpenEdit = (st: ShiftTemplate) => {
    setEditingShift(st);
    setName(st.name);
    setStartTime(st.startTime.substring(0, 5));
    setEndTime(st.endTime.substring(0, 5));
    setDurationHours(st.durationHours.toString());
    setIsModalOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      const url = editingShift ? `/api/shift-templates/${editingShift.id}` : '/api/shift-templates';
      const method = editingShift ? 'PUT' : 'POST';

      const res = await fetch(url, {
        method,
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${localStorage.getItem('access_token')}`,
        },
        body: JSON.stringify({
          name,
          startTime: startTime.length === 5 ? `${startTime}:00` : startTime,
          endTime: endTime.length === 5 ? `${endTime}:00` : endTime,
          durationHours: parseFloat(durationHours),
        }),
      });

      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || 'Failed to save shift template');
      }

      setIsModalOpen(false);
      fetchShifts();
    } catch (err: any) {
      setError(err.message);
    }
  };

  const getShiftIcon = (name: string, isOvernight: boolean) => {
    if (isOvernight || name.toLowerCase().includes('night')) {
      return <Moon className="w-5 h-5 text-indigo-500" />;
    } else if (name.toLowerCase().includes('evening')) {
      return <Sunset className="w-5 h-5 text-amber-500" />;
    }
    return <Sun className="w-5 h-5 text-sky-500" />;
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Shift Templates</h1>
          <p className="text-sm text-slate-500">Standard work shifts used across hospital departments</p>
        </div>
        {(isAdmin || isScheduler) && (
          <button
            onClick={handleOpenCreate}
            className="flex items-center gap-2 px-4 py-2.5 bg-sky-600 hover:bg-sky-700 text-white font-semibold rounded-xl shadow-md shadow-sky-600/20 transition-all cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>New Shift Template</span>
          </button>
        )}
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

      {/* Grid of Shift Templates */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        {shifts.map((st) => (
          <div key={st.id} className="glass-card rounded-2xl p-6 flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between mb-4">
                <div className="w-10 h-10 rounded-xl bg-slate-100 flex items-center justify-center">
                  {getShiftIcon(st.name, st.isOvernight)}
                </div>
                {st.isOvernight && (
                  <span className="badge badge-purple flex items-center gap-1">
                    <Moon className="w-3 h-3" />
                    <span>Overnight</span>
                  </span>
                )}
              </div>
              <h3 className="text-lg font-bold text-slate-900">{st.name} Shift</h3>

              <div className="mt-4 space-y-2">
                <div className="flex items-center justify-between text-sm">
                  <span className="text-slate-500">Hours</span>
                  <span className="font-semibold text-slate-800">{st.startTime} – {st.endTime}</span>
                </div>
                <div className="flex items-center justify-between text-sm">
                  <span className="text-slate-500">Duration</span>
                  <span className="badge badge-info">{st.durationHours} Hours</span>
                </div>
              </div>
            </div>

            {(isAdmin || isScheduler) && (
              <div className="mt-6 pt-4 border-t border-slate-100 flex justify-end">
                <button
                  onClick={() => handleOpenEdit(st)}
                  className="px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-semibold cursor-pointer"
                >
                  Edit Shift
                </button>
              </div>
            )}
          </div>
        ))}
      </div>

      {/* Create / Edit Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-xs">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-100">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-bold text-slate-900">
                {editingShift ? 'Edit Shift Template' : 'New Shift Template'}
              </h3>
              <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Shift Name</label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Morning, Evening, Night"
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Start Time</label>
                  <input
                    type="time"
                    required
                    value={startTime}
                    onChange={(e) => setStartTime(e.target.value)}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">End Time</label>
                  <input
                    type="time"
                    required
                    value={endTime}
                    onChange={(e) => setEndTime(e.target.value)}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Duration (Hours)</label>
                <input
                  type="number"
                  step="0.25"
                  required
                  value={durationHours}
                  onChange={(e) => setDurationHours(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                />
                <p className="text-[11px] text-slate-400 mt-1">Duration is the source of truth for shift length calculations.</p>
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
                  Save Shift
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
