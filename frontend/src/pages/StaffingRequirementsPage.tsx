import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { Plus, Layers, AlertTriangle, Trash2, X, AlertCircle } from 'lucide-react';

interface Department {
  id: string;
  name: string;
}

interface ShiftTemplate {
  id: string;
  name: string;
}

interface Skill {
  id: string;
  name: string;
}

interface StaffingRequirement {
  id: string;
  departmentId: string;
  departmentName: string;
  shiftTemplateId: string;
  shiftTemplateName: string;
  shiftStartTime: string;
  shiftEndTime: string;
  shiftDate: string;
  employeeType: string;
  requiredSkillId?: string;
  requiredSkillName?: string;
  requiredCount: number;
  isExplicitZero: boolean;
}

interface StaffingWarning {
  requirementId: string;
  departmentId: string;
  departmentName: string;
  shiftDate: string;
  shiftTemplateName: string;
  employeeType: string;
  skillId: string;
  skillName: string;
  warningMessage: string;
  activeQualifiedEmployeeCount: number;
}

export const StaffingRequirementsPage: React.FC = () => {
  const { user, isAdmin, isScheduler, isDeptHead } = useAuth();

  const [departments, setDepartments] = useState<Department[]>([]);
  const [shiftTemplates, setShiftTemplates] = useState<ShiftTemplate[]>([]);
  const [skills, setSkills] = useState<Skill[]>([]);
  const [requirements, setRequirements] = useState<StaffingRequirement[]>([]);
  const [warnings, setWarnings] = useState<StaffingWarning[]>([]);

  const [selectedDept, setSelectedDept] = useState<string>('');
  const [startDate, setStartDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [endDate, setEndDate] = useState<string>(
    new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  );

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Modal
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingReq, setEditingReq] = useState<StaffingRequirement | null>(null);
  const [deptId, setDeptId] = useState('');
  const [shiftId, setShiftId] = useState('');
  const [shiftDate, setShiftDate] = useState(new Date().toISOString().split('T')[0]);
  const [employeeType, setEmployeeType] = useState('NURSE');
  const [requiredSkillId, setRequiredSkillId] = useState('');
  const [requiredCount, setRequiredCount] = useState('2');

  const fetchDropdowns = async () => {
    try {
      const headers = { Authorization: `Bearer ${localStorage.getItem('access_token')}` };
      const [deptRes, shiftRes, skillRes] = await Promise.all([
        fetch('/api/departments', { headers }),
        fetch('/api/shift-templates', { headers }),
        fetch('/api/skills', { headers }),
      ]);

      if (deptRes.ok) {
        const depts = await deptRes.json();
        setDepartments(depts);
        if (depts.length > 0 && !selectedDept) {
          // If Dept Head, auto-select own department
          if (user?.role === 'DEPT_HEAD' && user.departmentId) {
            setSelectedDept(user.departmentId);
          } else {
            setSelectedDept(depts[0].id);
          }
        }
      }
      if (shiftRes.ok) setShiftTemplates(await shiftRes.json());
      if (skillRes.ok) setSkills(await skillRes.json());
    } catch (e) {
      console.error('Failed to load dropdowns', e);
    }
  };

  const fetchRequirementsAndWarnings = async () => {
    setLoading(true);
    setError(null);
    try {
      const headers = { Authorization: `Bearer ${localStorage.getItem('access_token')}` };
      const query = new URLSearchParams();
      if (selectedDept) query.append('departmentId', selectedDept);
      if (startDate) query.append('startDate', startDate);
      if (endDate) query.append('endDate', endDate);

      const [reqRes, warnRes] = await Promise.all([
        fetch(`/api/staffing-requirements?${query.toString()}`, { headers }),
        fetch(`/api/staffing-requirements/warnings?${query.toString()}`, { headers }),
      ]);

      if (!reqRes.ok) {
        const err = await reqRes.json();
        throw new Error(err.message || 'Failed to fetch requirements');
      }

      setRequirements(await reqRes.json());
      if (warnRes.ok) {
        setWarnings(await warnRes.json());
      }
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDropdowns();
  }, []);

  useEffect(() => {
    fetchRequirementsAndWarnings();
  }, [selectedDept, startDate, endDate]);

  const handleOpenCreate = () => {
    setEditingReq(null);
    setDeptId(selectedDept || departments[0]?.id || '');
    setShiftId(shiftTemplates[0]?.id || '');
    setShiftDate(new Date().toISOString().split('T')[0]);
    setEmployeeType('NURSE');
    setRequiredSkillId('');
    setRequiredCount('2');
    setIsModalOpen(true);
  };

  const handleOpenEdit = (req: StaffingRequirement) => {
    setEditingReq(req);
    setDeptId(req.departmentId);
    setShiftId(req.shiftTemplateId);
    setShiftDate(req.shiftDate);
    setEmployeeType(req.employeeType);
    setRequiredSkillId(req.requiredSkillId || '');
    setRequiredCount(req.requiredCount.toString());
    setIsModalOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      const payload = {
        departmentId: deptId,
        shiftTemplateId: shiftId,
        shiftDate,
        employeeType,
        requiredSkillId: requiredSkillId || null,
        requiredCount: parseInt(requiredCount),
      };

      const url = editingReq ? `/api/staffing-requirements/${editingReq.id}` : '/api/staffing-requirements';
      const method = editingReq ? 'PUT' : 'POST';

      const res = await fetch(url, {
        method,
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${localStorage.getItem('access_token')}`,
        },
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || 'Failed to save staffing requirement');
      }

      setIsModalOpen(false);
      fetchRequirementsAndWarnings();
    } catch (err: any) {
      setError(err.message);
    }
  };

  const handleDelete = async (id: string) => {
    try {
      const res = await fetch(`/api/staffing-requirements/${id}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${localStorage.getItem('access_token')}` },
      });
      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || 'Failed to delete requirement');
      }
      fetchRequirementsAndWarnings();
    } catch (err: any) {
      setError(err.message);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Staffing Coverage Requirements</h1>
          <p className="text-sm text-slate-500">Define minimum required staff counts per department, shift, and skill qualification</p>
        </div>
        {(isAdmin || isScheduler || isDeptHead) && (
          <button
            onClick={handleOpenCreate}
            className="flex items-center gap-2 px-4 py-2.5 bg-sky-600 hover:bg-sky-700 text-white font-semibold rounded-xl shadow-md shadow-sky-600/20 transition-all cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>Add Coverage Requirement</span>
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

      {/* Prominent Zero-Qualified-Staff Sanity Check Warning Banner */}
      {warnings.length > 0 && (
        <div className="p-4 rounded-2xl bg-amber-50 border border-amber-300 text-amber-900 shadow-sm animate-in fade-in duration-200">
          <div className="flex items-start gap-3">
            <AlertTriangle className="w-6 h-6 text-amber-600 shrink-0 mt-0.5" />
            <div className="space-y-1">
              <h4 className="text-sm font-bold text-amber-900">
                Pre-Generation Sanity Warning: {warnings.length} requirement(s) reference skills with ZERO qualified active staff!
              </h4>
              <ul className="text-xs text-amber-800 space-y-1 list-disc list-inside">
                {warnings.map((w, idx) => (
                  <li key={idx}>
                    <span className="font-semibold">{w.departmentName}</span> ({w.shiftDate}) – Requires skill{' '}
                    <span className="font-bold underline">{w.skillName}</span> for {w.employeeType}, but 0 active employees hold this certification.
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </div>
      )}

      {/* Filter Bar */}
      <div className="glass-panel rounded-2xl p-4 flex flex-wrap items-center gap-3">
        <div>
          <label className="block text-xs text-slate-500 mb-1">Department</label>
          <select
            value={selectedDept}
            disabled={user?.role === 'DEPT_HEAD'}
            onChange={(e) => setSelectedDept(e.target.value)}
            className="px-3 py-2 bg-white border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500 min-w-[180px]"
          >
            <option value="">All Departments</option>
            {departments.map((d) => (
              <option key={d.id} value={d.id}>{d.name}</option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-xs text-slate-500 mb-1">Start Date</label>
          <input
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            className="px-3 py-2 bg-white border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
          />
        </div>

        <div>
          <label className="block text-xs text-slate-500 mb-1">End Date</label>
          <input
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            className="px-3 py-2 bg-white border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
          />
        </div>
      </div>

      {/* Table */}
      <div className="glass-card rounded-2xl overflow-hidden">
        {loading ? (
          <div className="p-12 text-center text-slate-500">Loading requirements...</div>
        ) : requirements.length === 0 ? (
          <div className="p-12 text-center text-slate-500">
            <Layers className="w-12 h-12 text-slate-300 mx-auto mb-3" />
            <p className="font-semibold text-slate-700">No staffing requirements set for this range</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200 text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  <th className="p-4">Date & Shift</th>
                  <th className="p-4">Department</th>
                  <th className="p-4">Staff Role Required</th>
                  <th className="p-4">Skill Qualification</th>
                  <th className="p-4">Required Count</th>
                  <th className="p-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {requirements.map((req) => (
                  <tr key={req.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="p-4">
                      <div className="font-bold text-slate-900">{req.shiftDate}</div>
                      <div className="text-xs text-slate-500">{req.shiftTemplateName} ({req.shiftStartTime} - {req.shiftEndTime})</div>
                    </td>
                    <td className="p-4 font-semibold text-slate-800">{req.departmentName}</td>
                    <td className="p-4">
                      <span className="badge badge-info">{req.employeeType}</span>
                    </td>
                    <td className="p-4">
                      {req.requiredSkillName ? (
                        <span className="badge badge-purple">{req.requiredSkillName}</span>
                      ) : (
                        <span className="text-xs text-slate-400">None Specified</span>
                      )}
                    </td>
                    <td className="p-4">
                      {req.isExplicitZero ? (
                        <span className="badge badge-danger">EXPLICIT 0 REQUIRED</span>
                      ) : (
                        <span className="badge badge-active">{req.requiredCount} Staff Needed</span>
                      )}
                    </td>
                    <td className="p-4 text-right">
                      {(isAdmin || isScheduler || (isDeptHead && req.departmentId === user?.departmentId)) && (
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => handleOpenEdit(req)}
                            className="px-2.5 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-medium cursor-pointer"
                          >
                            Edit
                          </button>
                          <button
                            onClick={() => handleDelete(req.id)}
                            className="px-2.5 py-1 bg-rose-50 hover:bg-rose-100 text-rose-700 rounded-lg text-xs font-medium cursor-pointer"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      )}
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
              <h3 className="text-lg font-bold text-slate-900">
                {editingReq ? 'Edit Coverage Requirement' : 'Add Coverage Requirement'}
              </h3>
              <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Department</label>
                <select
                  required
                  value={deptId}
                  disabled={user?.role === 'DEPT_HEAD'}
                  onChange={(e) => setDeptId(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                >
                  {departments.map((d) => (
                    <option key={d.id} value={d.id}>{d.name}</option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Shift Template</label>
                  <select
                    required
                    value={shiftId}
                    onChange={(e) => setShiftId(e.target.value)}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                  >
                    {shiftTemplates.map((s) => (
                      <option key={s.id} value={s.id}>{s.name}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Shift Date</label>
                  <input
                    type="date"
                    required
                    value={shiftDate}
                    onChange={(e) => setShiftDate(e.target.value)}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Employee Type</label>
                  <select
                    value={employeeType}
                    onChange={(e) => setEmployeeType(e.target.value)}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                  >
                    <option value="DOCTOR">Doctor</option>
                    <option value="NURSE">Nurse</option>
                    <option value="LAB_TECH">Lab Tech</option>
                    <option value="PHARMACIST">Pharmacist</option>
                    <option value="SUPPORT">Support</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Required Count</label>
                  <input
                    type="number"
                    min="0"
                    required
                    value={requiredCount}
                    onChange={(e) => setRequiredCount(e.target.value)}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Required Skill (Optional)</label>
                <select
                  value={requiredSkillId}
                  onChange={(e) => setRequiredSkillId(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                >
                  <option value="">None (Any Skill)</option>
                  {skills.map((s) => (
                    <option key={s.id} value={s.id}>{s.name}</option>
                  ))}
                </select>
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
                  Save Requirement
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
