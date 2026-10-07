import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { Plus, Search, UserX, Award, RefreshCw, X, AlertCircle } from 'lucide-react';

interface Department {
  id: string;
  name: string;
}

interface Skill {
  id: string;
  name: string;
}

interface EmployeeSkill {
  skillId: string;
  skillName: string;
  certifiedDate?: string;
  expiryDate?: string;
  isExpired: boolean;
}

interface Employee {
  id: string;
  firstName: string;
  lastName: string;
  fullName: string;
  employeeType: string;
  departmentId: string;
  departmentName: string;
  contactEmail: string;
  contactPhone?: string;
  employmentStatus: string;
  hireDate: string;
  maxWeeklyHoursOverride?: number;
}

export const EmployeesPage: React.FC = () => {
  const { isAdmin, isHr } = useAuth();
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [allSkills, setAllSkills] = useState<Skill[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Search & Filter
  const [search, setSearch] = useState('');
  const [selectedDept, setSelectedDept] = useState('');
  const [selectedType, setSelectedType] = useState('');
  const [selectedStatus, setSelectedStatus] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  // Modals
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingEmp, setEditingEmp] = useState<Employee | null>(null);
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    employeeType: 'NURSE',
    departmentId: '',
    contactEmail: '',
    contactPhone: '',
    employmentStatus: 'ACTIVE',
    hireDate: new Date().toISOString().split('T')[0],
    maxWeeklyHoursOverride: '',
  });

  // Skills Modal
  const [skillsEmp, setSkillsEmp] = useState<Employee | null>(null);
  const [empSkills, setEmpSkills] = useState<EmployeeSkill[]>([]);
  const [assignSkillId, setAssignSkillId] = useState('');
  const [certifiedDate, setCertifiedDate] = useState('');
  const [expiryDate, setExpiryDate] = useState('');

  // Termination Confirm Dialog
  const [terminateEmp, setTerminateEmp] = useState<Employee | null>(null);

  const fetchDepartments = async () => {
    try {
      const res = await fetch('/api/departments', {
        headers: { Authorization: `Bearer ${localStorage.getItem('access_token')}` },
      });
      if (res.ok) {
        const data = await res.json();
        setDepartments(data);
      }
    } catch (e) {
      console.error('Failed to load departments', e);
    }
  };

  const fetchSkills = async () => {
    try {
      const res = await fetch('/api/skills', {
        headers: { Authorization: `Bearer ${localStorage.getItem('access_token')}` },
      });
      if (res.ok) {
        const data = await res.json();
        setAllSkills(data);
      }
    } catch (e) {
      console.error('Failed to load skills', e);
    }
  };

  const fetchEmployees = async () => {
    setLoading(true);
    setError(null);
    try {
      const query = new URLSearchParams();
      if (search) query.append('search', search);
      if (selectedDept) query.append('departmentId', selectedDept);
      if (selectedType) query.append('employeeType', selectedType);
      if (selectedStatus) query.append('status', selectedStatus);
      query.append('page', page.toString());
      query.append('size', '10');

      const res = await fetch(`/api/employees?${query.toString()}`, {
        headers: { Authorization: `Bearer ${localStorage.getItem('access_token')}` },
      });

      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || 'Failed to fetch employees');
      }

      const data = await res.json();
      setEmployees(data.content || []);
      setTotalPages(data.totalPages || 1);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDepartments();
    fetchSkills();
  }, []);

  useEffect(() => {
    fetchEmployees();
  }, [search, selectedDept, selectedType, selectedStatus, page]);

  const handleOpenCreate = () => {
    setEditingEmp(null);
    setFormData({
      firstName: '',
      lastName: '',
      employeeType: 'NURSE',
      departmentId: departments[0]?.id || '',
      contactEmail: '',
      contactPhone: '',
      employmentStatus: 'ACTIVE',
      hireDate: new Date().toISOString().split('T')[0],
      maxWeeklyHoursOverride: '',
    });
    setIsFormOpen(true);
  };

  const handleOpenEdit = (emp: Employee) => {
    setEditingEmp(emp);
    setFormData({
      firstName: emp.firstName,
      lastName: emp.lastName,
      employeeType: emp.employeeType,
      departmentId: emp.departmentId,
      contactEmail: emp.contactEmail,
      contactPhone: emp.contactPhone || '',
      employmentStatus: emp.employmentStatus,
      hireDate: emp.hireDate,
      maxWeeklyHoursOverride: emp.maxWeeklyHoursOverride ? emp.maxWeeklyHoursOverride.toString() : '',
    });
    setIsFormOpen(true);
  };

  const handleFormSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const payload = {
      ...formData,
      maxWeeklyHoursOverride: formData.maxWeeklyHoursOverride ? parseInt(formData.maxWeeklyHoursOverride) : null,
    };

    try {
      const url = editingEmp ? `/api/employees/${editingEmp.id}` : '/api/employees';
      const method = editingEmp ? 'PUT' : 'POST';

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
        throw new Error(err.message || 'Operation failed');
      }

      setIsFormOpen(false);
      fetchEmployees();
    } catch (err: any) {
      setError(err.message);
    }
  };

  const handleConfirmTerminate = async () => {
    if (!terminateEmp) return;
    try {
      const res = await fetch(`/api/employees/${terminateEmp.id}/terminate`, {
        method: 'PATCH',
        headers: { Authorization: `Bearer ${localStorage.getItem('access_token')}` },
      });

      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || 'Failed to terminate employee');
      }

      setTerminateEmp(null);
      fetchEmployees();
    } catch (err: any) {
      setError(err.message);
      setTerminateEmp(null);
    }
  };

  const handleOpenSkillsModal = async (emp: Employee) => {
    setSkillsEmp(emp);
    setAssignSkillId(allSkills[0]?.id || '');
    setCertifiedDate(new Date().toISOString().split('T')[0]);
    setExpiryDate('');
    fetchEmployeeSkills(emp.id);
  };

  const fetchEmployeeSkills = async (employeeId: string) => {
    try {
      const res = await fetch(`/api/employees/${employeeId}/skills`, {
        headers: { Authorization: `Bearer ${localStorage.getItem('access_token')}` },
      });
      if (res.ok) {
        const data = await res.json();
        setEmpSkills(data);
      }
    } catch (e) {
      console.error(e);
    }
  };

  const handleAssignSkill = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!skillsEmp || !assignSkillId) return;

    try {
      const res = await fetch(`/api/employees/${skillsEmp.id}/skills`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${localStorage.getItem('access_token')}`,
        },
        body: JSON.stringify({
          skillId: assignSkillId,
          certifiedDate: certifiedDate || null,
          expiryDate: expiryDate || null,
        }),
      });

      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || 'Failed to assign skill');
      }

      fetchEmployeeSkills(skillsEmp.id);
    } catch (err: any) {
      setError(err.message);
    }
  };

  const handleRemoveSkill = async (skillId: string) => {
    if (!skillsEmp) return;
    try {
      const res = await fetch(`/api/employees/${skillsEmp.id}/skills/${skillId}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${localStorage.getItem('access_token')}` },
      });
      if (res.ok) {
        fetchEmployeeSkills(skillsEmp.id);
      }
    } catch (e) {
      console.error(e);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Employee Directory</h1>
          <p className="text-sm text-slate-500">Manage hospital staff profiles, contact details, and skill certifications</p>
        </div>
        {(isAdmin || isHr) && (
          <button
            onClick={handleOpenCreate}
            className="flex items-center gap-2 px-4 py-2.5 bg-sky-600 hover:bg-sky-700 text-white font-semibold rounded-xl shadow-md shadow-sky-600/20 transition-all cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>Add New Employee</span>
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

      {/* Filter Bar */}
      <div className="glass-panel rounded-2xl p-4 flex flex-wrap items-center gap-3">
        <div className="relative flex-1 min-w-[200px]">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
          <input
            type="text"
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(0); }}
            placeholder="Search by name or email..."
            className="w-full pl-9 pr-4 py-2 bg-white border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
          />
        </div>

        <select
          value={selectedDept}
          onChange={(e) => { setSelectedDept(e.target.value); setPage(0); }}
          className="px-3 py-2 bg-white border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
        >
          <option value="">All Departments</option>
          {departments.map((d) => (
            <option key={d.id} value={d.id}>{d.name}</option>
          ))}
        </select>

        <select
          value={selectedType}
          onChange={(e) => { setSelectedType(e.target.value); setPage(0); }}
          className="px-3 py-2 bg-white border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
        >
          <option value="">All Staff Types</option>
          <option value="DOCTOR">Doctor</option>
          <option value="NURSE">Nurse</option>
          <option value="LAB_TECH">Lab Tech</option>
          <option value="PHARMACIST">Pharmacist</option>
          <option value="SUPPORT">Support</option>
        </select>

        <select
          value={selectedStatus}
          onChange={(e) => { setSelectedStatus(e.target.value); setPage(0); }}
          className="px-3 py-2 bg-white border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
        >
          <option value="">All Statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="ON_LEAVE">On Leave</option>
          <option value="TERMINATED">Terminated</option>
        </select>
      </div>

      {/* Employees Table */}
      <div className="glass-card rounded-2xl overflow-hidden">
        {loading ? (
          <div className="p-12 text-center text-slate-500 flex items-center justify-center gap-2">
            <RefreshCw className="w-5 h-5 animate-spin text-sky-600" />
            <span>Loading employees...</span>
          </div>
        ) : employees.length === 0 ? (
          <div className="p-12 text-center text-slate-500">
            <UserX className="w-12 h-12 text-slate-300 mx-auto mb-3" />
            <p className="font-semibold text-slate-700">No employees found</p>
            <p className="text-xs text-slate-400 mt-1">Try adjusting search filters or adding a new employee.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200 text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  <th className="p-4">Employee</th>
                  <th className="p-4">Role / Type</th>
                  <th className="p-4">Department</th>
                  <th className="p-4">Contact</th>
                  <th className="p-4">Status</th>
                  <th className="p-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {employees.map((emp) => (
                  <tr key={emp.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="p-4">
                      <div className="font-bold text-slate-900">{emp.fullName}</div>
                      <div className="text-xs text-slate-400">Hired: {emp.hireDate}</div>
                    </td>
                    <td className="p-4">
                      <span className="badge badge-info">{emp.employeeType}</span>
                      {emp.maxWeeklyHoursOverride && (
                        <div className="text-[11px] text-slate-400 mt-0.5">Max {emp.maxWeeklyHoursOverride}h/wk</div>
                      )}
                    </td>
                    <td className="p-4 text-slate-700 font-medium">{emp.departmentName || 'Unassigned'}</td>
                    <td className="p-4">
                      <div className="text-slate-800">{emp.contactEmail}</div>
                      <div className="text-xs text-slate-400">{emp.contactPhone || 'No phone'}</div>
                    </td>
                    <td className="p-4">
                      <span
                        className={`badge ${
                          emp.employmentStatus === 'ACTIVE'
                            ? 'badge-active'
                            : emp.employmentStatus === 'ON_LEAVE'
                            ? 'badge-warning'
                            : 'badge-danger'
                        }`}
                      >
                        {emp.employmentStatus}
                      </span>
                    </td>
                    <td className="p-4 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => handleOpenSkillsModal(emp)}
                          className="px-2.5 py-1.5 bg-purple-50 hover:bg-purple-100 text-purple-700 font-semibold rounded-lg text-xs flex items-center gap-1 cursor-pointer"
                          title="Manage Skills"
                        >
                          <Award className="w-3.5 h-3.5" />
                          <span>Skills</span>
                        </button>

                        {(isAdmin || isHr) && (
                          <button
                            onClick={() => handleOpenEdit(emp)}
                            className="px-2.5 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold rounded-lg text-xs cursor-pointer"
                          >
                            Edit
                          </button>
                        )}

                        {isAdmin && emp.employmentStatus !== 'TERMINATED' && (
                          <button
                            onClick={() => setTerminateEmp(emp)}
                            className="px-2.5 py-1.5 bg-rose-50 hover:bg-rose-100 text-rose-700 font-semibold rounded-lg text-xs cursor-pointer"
                          >
                            Terminate
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="p-4 bg-slate-50 border-t border-slate-200 flex items-center justify-between text-xs text-slate-500">
            <span>Page {page + 1} of {totalPages}</span>
            <div className="flex items-center gap-2">
              <button
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="px-3 py-1.5 bg-white border border-slate-200 rounded-lg font-medium disabled:opacity-50 cursor-pointer"
              >
                Previous
              </button>
              <button
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
                className="px-3 py-1.5 bg-white border border-slate-200 rounded-lg font-medium disabled:opacity-50 cursor-pointer"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Create / Edit Employee Modal */}
      {isFormOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-xs">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-slate-100 animate-in fade-in duration-150">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-bold text-slate-900">
                {editingEmp ? 'Edit Employee' : 'Add New Employee'}
              </h3>
              <button onClick={() => setIsFormOpen(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleFormSubmit} className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">First Name</label>
                  <input
                    type="text"
                    required
                    value={formData.firstName}
                    onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Last Name</label>
                  <input
                    type="text"
                    required
                    value={formData.lastName}
                    onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Staff Type</label>
                  <select
                    value={formData.employeeType}
                    onChange={(e) => setFormData({ ...formData, employeeType: e.target.value })}
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
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Department</label>
                  <select
                    required
                    value={formData.departmentId}
                    onChange={(e) => setFormData({ ...formData, departmentId: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                  >
                    <option value="">Select Department</option>
                    {departments.map((d) => (
                      <option key={d.id} value={d.id}>{d.name}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Contact Email</label>
                  <input
                    type="email"
                    required
                    value={formData.contactEmail}
                    onChange={(e) => setFormData({ ...formData, contactEmail: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Contact Phone</label>
                  <input
                    type="text"
                    value={formData.contactPhone}
                    onChange={(e) => setFormData({ ...formData, contactPhone: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Hire Date</label>
                  <input
                    type="date"
                    required
                    value={formData.hireDate}
                    onChange={(e) => setFormData({ ...formData, hireDate: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Max Weekly Hours Override</label>
                  <input
                    type="number"
                    min="8"
                    max="80"
                    value={formData.maxWeeklyHoursOverride}
                    onChange={(e) => setFormData({ ...formData, maxWeeklyHoursOverride: e.target.value })}
                    placeholder="e.g. 40"
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                  />
                </div>
              </div>

              <div className="flex items-center justify-end gap-3 pt-3">
                <button
                  type="button"
                  onClick={() => setIsFormOpen(false)}
                  className="px-4 py-2 text-sm font-medium text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-xl cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 text-sm font-semibold text-white bg-sky-600 hover:bg-sky-700 rounded-xl shadow-md cursor-pointer"
                >
                  Save Employee
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Skills Assignment Modal */}
      {skillsEmp && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-xs">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-slate-100 animate-in fade-in duration-150">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h3 className="text-lg font-bold text-slate-900">Manage Employee Certifications</h3>
                <p className="text-xs text-slate-500">Skills assigned to {skillsEmp.fullName}</p>
              </div>
              <button onClick={() => setSkillsEmp(null)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Currently assigned skills list */}
            <div className="mb-6 space-y-2 max-h-48 overflow-y-auto pr-1">
              {empSkills.length === 0 ? (
                <p className="text-xs text-slate-400 italic">No skills currently assigned to this employee.</p>
              ) : (
                empSkills.map((es) => (
                  <div key={es.skillId} className="flex items-center justify-between p-3 bg-slate-50 rounded-xl border border-slate-200">
                    <div>
                      <div className="text-sm font-semibold text-slate-800 flex items-center gap-2">
                        <span>{es.skillName}</span>
                        {es.isExpired ? (
                          <span className="badge badge-danger">EXPIRED</span>
                        ) : (
                          <span className="badge badge-active">VALID</span>
                        )}
                      </div>
                      <div className="text-xs text-slate-500">
                        Expires: {es.expiryDate || 'No expiry'}
                      </div>
                    </div>
                    {(isAdmin || isHr) && (
                      <button
                        onClick={() => handleRemoveSkill(es.skillId)}
                        className="text-xs text-rose-600 hover:text-rose-800 font-medium px-2 py-1 bg-rose-50 hover:bg-rose-100 rounded-lg cursor-pointer"
                      >
                        Remove
                      </button>
                    )}
                  </div>
                ))
              )}
            </div>

            {/* Add Skill Form */}
            {(isAdmin || isHr) && (
              <form onSubmit={handleAssignSkill} className="border-t border-slate-200 pt-4 space-y-3">
                <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wider">Assign New Skill</h4>
                <div className="grid grid-cols-1 gap-2">
                  <select
                    value={assignSkillId}
                    onChange={(e) => setAssignSkillId(e.target.value)}
                    required
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                  >
                    {allSkills.map((s) => (
                      <option key={s.id} value={s.id}>{s.name}</option>
                    ))}
                  </select>
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="block text-xs text-slate-500 mb-1">Certified Date</label>
                    <input
                      type="date"
                      value={certifiedDate}
                      onChange={(e) => setCertifiedDate(e.target.value)}
                      className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-xs text-slate-500 mb-1">Expiry Date</label>
                    <input
                      type="date"
                      value={expiryDate}
                      onChange={(e) => setExpiryDate(e.target.value)}
                      className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm"
                    />
                  </div>
                </div>
                <button
                  type="submit"
                  className="w-full py-2.5 bg-purple-600 hover:bg-purple-700 text-white font-semibold rounded-xl text-sm shadow-md cursor-pointer"
                >
                  Assign Skill
                </button>
              </form>
            )}
          </div>
        </div>
      )}

      {/* Terminate Confirm Dialog */}
      <ConfirmDialog
        isOpen={!!terminateEmp}
        title="Terminate Employee"
        message={`Are you sure you want to terminate ${terminateEmp?.fullName}? Their status will transition to TERMINATED. This action will be audited.`}
        confirmLabel="Terminate Employee"
        isDanger={true}
        onConfirm={handleConfirmTerminate}
        onCancel={() => setTerminateEmp(null)}
      />
    </div>
  );
};
