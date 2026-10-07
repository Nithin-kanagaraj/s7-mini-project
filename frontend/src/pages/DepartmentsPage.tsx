import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { Plus, Building, Users, AlertCircle, X } from 'lucide-react';

interface Department {
  id: string;
  name: string;
  description?: string;
  isActive: boolean;
  employeeCount: number;
}

export const DepartmentsPage: React.FC = () => {
  const { isAdmin } = useAuth();
  const [departments, setDepartments] = useState<Department[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingDept, setEditingDept] = useState<Department | null>(null);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [isActive, setIsActive] = useState(true);

  const [deletingDept, setDeletingDept] = useState<Department | null>(null);

  const fetchDepartments = async () => {
    setLoading(true);
    try {
      const res = await fetch('/api/departments', {
        headers: { Authorization: `Bearer ${localStorage.getItem('access_token')}` },
      });
      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || 'Failed to load departments');
      }
      const data = await res.json();
      setDepartments(data);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDepartments();
  }, []);

  const handleOpenCreate = () => {
    setEditingDept(null);
    setName('');
    setDescription('');
    setIsActive(true);
    setIsModalOpen(true);
  };

  const handleOpenEdit = (dept: Department) => {
    setEditingDept(dept);
    setName(dept.name);
    setDescription(dept.description || '');
    setIsActive(dept.isActive);
    setIsModalOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      const url = editingDept ? `/api/departments/${editingDept.id}` : '/api/departments';
      const method = editingDept ? 'PUT' : 'POST';

      const res = await fetch(url, {
        method,
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${localStorage.getItem('access_token')}`,
        },
        body: JSON.stringify({ name, description, isActive }),
      });

      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || 'Failed to save department');
      }

      setIsModalOpen(false);
      fetchDepartments();
    } catch (err: any) {
      setError(err.message);
    }
  };

  const handleConfirmDelete = async () => {
    if (!deletingDept) return;
    try {
      const res = await fetch(`/api/departments/${deletingDept.id}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${localStorage.getItem('access_token')}` },
      });

      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || 'Failed to remove department');
      }

      setDeletingDept(null);
      fetchDepartments();
    } catch (err: any) {
      setError(err.message);
      setDeletingDept(null);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Hospital Departments</h1>
          <p className="text-sm text-slate-500">Overview of medical units, inpatient wards, and clinical services</p>
        </div>
        {isAdmin && (
          <button
            onClick={handleOpenCreate}
            className="flex items-center gap-2 px-4 py-2.5 bg-sky-600 hover:bg-sky-700 text-white font-semibold rounded-xl shadow-md shadow-sky-600/20 transition-all cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>Create Department</span>
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

      {/* Grid of Department Cards */}
      {loading ? (
        <div className="p-12 text-center text-slate-500">Loading departments...</div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {departments.map((dept) => (
            <div key={dept.id} className="glass-card rounded-2xl p-6 flex flex-col justify-between">
              <div>
                <div className="flex items-center justify-between mb-3">
                  <div className="w-10 h-10 rounded-xl bg-sky-100 text-sky-700 flex items-center justify-center font-bold">
                    <Building className="w-5 h-5" />
                  </div>
                  <span className={`badge ${dept.isActive ? 'badge-active' : 'badge-danger'}`}>
                    {dept.isActive ? 'Active' : 'Inactive'}
                  </span>
                </div>
                <h3 className="text-lg font-bold text-slate-900">{dept.name}</h3>
                <p className="text-xs text-slate-500 mt-1 leading-relaxed">{dept.description || 'No description provided.'}</p>
              </div>

              <div className="mt-6 pt-4 border-t border-slate-100 flex items-center justify-between">
                <div className="flex items-center gap-1.5 text-xs text-slate-600 font-semibold">
                  <Users className="w-4 h-4 text-slate-400" />
                  <span>{dept.employeeCount} Assigned Staff</span>
                </div>

                {isAdmin && (
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => handleOpenEdit(dept)}
                      className="px-2.5 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-medium cursor-pointer"
                    >
                      Edit
                    </button>
                    <button
                      onClick={() => setDeletingDept(dept)}
                      className="px-2.5 py-1 bg-rose-50 hover:bg-rose-100 text-rose-700 rounded-lg text-xs font-medium cursor-pointer"
                    >
                      {dept.employeeCount > 0 ? 'Deactivate' : 'Delete'}
                    </button>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create / Edit Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-xs">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-100">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-bold text-slate-900">
                {editingDept ? 'Edit Department' : 'Create Department'}
              </h3>
              <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Department Name</label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Description</label>
                <textarea
                  rows={3}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                />
              </div>

              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  id="isActive"
                  checked={isActive}
                  onChange={(e) => setIsActive(e.target.checked)}
                  className="rounded border-slate-300 text-sky-600 focus:ring-sky-500"
                />
                <label htmlFor="isActive" className="text-sm font-medium text-slate-700">Active Department</label>
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
                  Save Department
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete / Soft-Deactivate Dialog */}
      <ConfirmDialog
        isOpen={!!deletingDept}
        title={deletingDept && deletingDept.employeeCount > 0 ? "Soft-Deactivate Department" : "Delete Department"}
        message={
          deletingDept && deletingDept.employeeCount > 0
            ? `Department '${deletingDept.name}' has ${deletingDept.employeeCount} assigned employees. It will be soft-deactivated (is_active=false) instead of permanently deleted.`
            : `Are you sure you want to delete department '${deletingDept?.name}'?`
        }
        confirmLabel={deletingDept && deletingDept.employeeCount > 0 ? "Deactivate" : "Delete"}
        isDanger={true}
        onConfirm={handleConfirmDelete}
        onCancel={() => setDeletingDept(null)}
      />
    </div>
  );
};
