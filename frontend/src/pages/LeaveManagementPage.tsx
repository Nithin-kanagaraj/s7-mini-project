import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { Plus, FileText, CheckCircle2, XCircle, AlertTriangle, AlertCircle, X, Clock } from 'lucide-react';

interface LeaveRequest {
  id: string;
  employeeId: string;
  employeeName: string;
  departmentId: string;
  departmentName: string;
  leaveType: string;
  startDate: string;
  endDate: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  approvedByName?: string;
  requestedAt: string;
  decidedAt?: string;
  isRetroactive: boolean;
  hasOverlapWarning?: boolean;
  overlapWarningMessage?: string;
}

export const LeaveManagementPage: React.FC = () => {
  const { user, isAdmin, isHr, isDeptHead } = useAuth();

  const [myRequests, setMyRequests] = useState<LeaveRequest[]>([]);
  const [allRequests, setAllRequests] = useState<LeaveRequest[]>([]);
  const [activeTab, setActiveTab] = useState<'my' | 'queue'>('my');

  const [error, setError] = useState<string | null>(null);

  // Submit Modal
  const [isSubmitOpen, setIsSubmitOpen] = useState(false);
  const [leaveType, setLeaveType] = useState('ANNUAL');
  const [startDate, setStartDate] = useState(new Date().toISOString().split('T')[0]);
  const [endDate, setEndDate] = useState(new Date().toISOString().split('T')[0]);
  const [reason, setReason] = useState('');

  // Overlap Warning Prompt
  const [overlapWarning, setOverlapWarning] = useState<string | null>(null);

  // Decision Modal/Confirm
  const [decisionReq, setDecisionReq] = useState<LeaveRequest | null>(null);
  const [decisionAction, setDecisionAction] = useState<'APPROVED' | 'REJECTED'>('APPROVED');

  const isRetroactiveDate = startDate ? new Date(startDate) < new Date(new Date().toISOString().split('T')[0]) : false;

  const fetchMyRequests = async () => {
    if (!user?.employeeId) return;
    try {
      const res = await fetch('/api/leave/my-requests', {
        headers: { Authorization: `Bearer ${localStorage.getItem('access_token')}` },
      });
      if (res.ok) {
        setMyRequests(await res.json());
      }
    } catch (e) {
      console.error(e);
    }
  };

  const fetchAllRequests = async () => {
    if (!isAdmin && !isHr && !isDeptHead) return;
    try {
      const res = await fetch('/api/leave', {
        headers: { Authorization: `Bearer ${localStorage.getItem('access_token')}` },
      });
      if (res.ok) {
        setAllRequests(await res.json());
      }
    } catch (e) {
      console.error(e);
    }
  };

  useEffect(() => {
    fetchMyRequests();
    fetchAllRequests();
  }, [user]);

  const handleOpenSubmit = () => {
    setLeaveType('ANNUAL');
    setStartDate(new Date().toISOString().split('T')[0]);
    setEndDate(new Date().toISOString().split('T')[0]);
    setReason('');
    setOverlapWarning(null);
    setIsSubmitOpen(true);
  };

  const handleFormSubmit = async (e: React.FormEvent, confirmOverlap = false) => {
    e.preventDefault();
    setError(null);

    if (isRetroactiveDate && !reason.trim()) {
      setError('Retroactive leave request (start date in past) requires an explicit reason.');
      return;
    }

    try {
      const res = await fetch('/api/leave/my-requests', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${localStorage.getItem('access_token')}`,
        },
        body: JSON.stringify({
          leaveType,
          startDate,
          endDate,
          reason,
          confirmOverlap,
        }),
      });

      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.message || 'Failed to submit leave request');
      }

      if (data.hasOverlapWarning && !confirmOverlap) {
        setOverlapWarning(data.overlapWarningMessage || 'Warning: Overlapping leave request detected.');
        return;
      }

      setIsSubmitOpen(false);
      setOverlapWarning(null);
      fetchMyRequests();
      fetchAllRequests();
    } catch (err: any) {
      setError(err.message);
    }
  };

  const handleConfirmDecision = async () => {
    if (!decisionReq) return;
    try {
      const res = await fetch(`/api/leave/${decisionReq.id}/decide`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${localStorage.getItem('access_token')}`,
        },
        body: JSON.stringify({
          status: decisionAction,
        }),
      });

      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || 'Failed to record decision');
      }

      setDecisionReq(null);
      fetchMyRequests();
      fetchAllRequests();
    } catch (err: any) {
      setError(err.message);
      setDecisionReq(null);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Leave Management</h1>
          <p className="text-sm text-slate-500">Submit leave requests and process department approval queues</p>
        </div>
        <button
          onClick={handleOpenSubmit}
          className="flex items-center gap-2 px-4 py-2.5 bg-sky-600 hover:bg-sky-700 text-white font-semibold rounded-xl shadow-md shadow-sky-600/20 transition-all cursor-pointer"
        >
          <Plus className="w-4 h-4" />
          <span>Submit Leave Request</span>
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

      {/* Tabs */}
      {(isAdmin || isHr || isDeptHead) && (
        <div className="flex items-center gap-2 border-b border-slate-200">
          <button
            onClick={() => setActiveTab('my')}
            className={`pb-3 px-4 text-sm font-semibold border-b-2 cursor-pointer transition-colors ${
              activeTab === 'my'
                ? 'border-sky-600 text-sky-600'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            My Leave Requests ({myRequests.length})
          </button>
          <button
            onClick={() => setActiveTab('queue')}
            className={`pb-3 px-4 text-sm font-semibold border-b-2 cursor-pointer transition-colors ${
              activeTab === 'queue'
                ? 'border-sky-600 text-sky-600'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            Department Approval Queue ({allRequests.filter(r => r.status === 'PENDING').length} Pending)
          </button>
        </div>
      )}

      {/* Content */}
      <div className="glass-card rounded-2xl overflow-hidden">
        {activeTab === 'my' ? (
          <div>
            {myRequests.length === 0 ? (
              <div className="p-12 text-center text-slate-500">
                <FileText className="w-12 h-12 text-slate-300 mx-auto mb-3" />
                <p className="font-semibold text-slate-700">No leave requests submitted</p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse text-sm">
                  <thead>
                    <tr className="bg-slate-50 border-b border-slate-200 text-xs font-semibold text-slate-500 uppercase tracking-wider">
                      <th className="p-4">Type</th>
                      <th className="p-4">Dates</th>
                      <th className="p-4">Requested At</th>
                      <th className="p-4">Status</th>
                      <th className="p-4">Approver</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {myRequests.map((r) => (
                      <tr key={r.id} className="hover:bg-slate-50/80 transition-colors">
                        <td className="p-4">
                          <span className="badge badge-info">{r.leaveType}</span>
                          {r.isRetroactive && (
                            <span className="badge badge-warning ml-2">RETROACTIVE</span>
                          )}
                        </td>
                        <td className="p-4 font-bold text-slate-800">
                          {r.startDate} to {r.endDate}
                        </td>
                        <td className="p-4 text-xs text-slate-500">{new Date(r.requestedAt).toLocaleString()}</td>
                        <td className="p-4">
                          <span
                            className={`badge ${
                              r.status === 'APPROVED'
                                ? 'badge-active'
                                : r.status === 'PENDING'
                                ? 'badge-warning'
                                : 'badge-danger'
                            }`}
                          >
                            {r.status}
                          </span>
                        </td>
                        <td className="p-4 text-slate-600 text-xs">{r.approvedByName || '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        ) : (
          <div>
            {allRequests.length === 0 ? (
              <div className="p-12 text-center text-slate-500">No leave requests in approval queue</div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse text-sm">
                  <thead>
                    <tr className="bg-slate-50 border-b border-slate-200 text-xs font-semibold text-slate-500 uppercase tracking-wider">
                      <th className="p-4">Employee</th>
                      <th className="p-4">Department</th>
                      <th className="p-4">Type</th>
                      <th className="p-4">Dates</th>
                      <th className="p-4">Status</th>
                      <th className="p-4 text-right">Approval Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {allRequests.map((r) => (
                      <tr key={r.id} className="hover:bg-slate-50/80 transition-colors">
                        <td className="p-4">
                          <div className="font-bold text-slate-900">{r.employeeName}</div>
                          <div className="text-xs text-slate-400">Req: {new Date(r.requestedAt).toLocaleDateString()}</div>
                        </td>
                        <td className="p-4 font-semibold text-slate-800">{r.departmentName}</td>
                        <td className="p-4">
                          <span className="badge badge-info">{r.leaveType}</span>
                          {r.isRetroactive && <span className="badge badge-warning ml-2">RETROACTIVE</span>}
                        </td>
                        <td className="p-4 font-bold text-slate-800">{r.startDate} to {r.endDate}</td>
                        <td className="p-4">
                          <span
                            className={`badge ${
                              r.status === 'APPROVED'
                                ? 'badge-active'
                                : r.status === 'PENDING'
                                ? 'badge-warning'
                                : 'badge-danger'
                            }`}
                          >
                            {r.status}
                          </span>
                        </td>
                        <td className="p-4 text-right">
                          {r.status === 'PENDING' && (
                            <div className="flex items-center justify-end gap-2">
                              <button
                                onClick={() => { setDecisionReq(r); setDecisionAction('APPROVED'); }}
                                className="px-3 py-1 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 font-semibold rounded-lg text-xs flex items-center gap-1 cursor-pointer"
                              >
                                <CheckCircle2 className="w-3.5 h-3.5" />
                                <span>Approve</span>
                              </button>
                              <button
                                onClick={() => { setDecisionReq(r); setDecisionAction('REJECTED'); }}
                                className="px-3 py-1 bg-rose-50 hover:bg-rose-100 text-rose-700 font-semibold rounded-lg text-xs flex items-center gap-1 cursor-pointer"
                              >
                                <XCircle className="w-3.5 h-3.5" />
                                <span>Reject</span>
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
        )}
      </div>

      {/* Submit Leave Modal */}
      {isSubmitOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-xs">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-100">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-bold text-slate-900">Submit Leave Request</h3>
              <button onClick={() => setIsSubmitOpen(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Overlap Warning Prompt */}
            {overlapWarning ? (
              <div className="space-y-4">
                <div className="p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 text-sm flex items-start gap-3">
                  <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
                  <div>
                    <span className="font-bold">Overlapping Request Warning:</span>
                    <p className="mt-1 text-xs">{overlapWarning}</p>
                  </div>
                </div>
                <p className="text-xs text-slate-600">Do you want to confirm and submit this overlapping request anyway?</p>

                <div className="flex items-center justify-end gap-3 pt-2">
                  <button
                    onClick={() => setOverlapWarning(null)}
                    className="px-4 py-2 text-sm font-medium text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-xl cursor-pointer"
                  >
                    Back to Edit
                  </button>
                  <button
                    onClick={(e) => handleFormSubmit(e, true)}
                    className="px-4 py-2 text-sm font-semibold text-white bg-amber-600 hover:bg-amber-700 rounded-xl shadow-md cursor-pointer"
                  >
                    Confirm Overlapping Submission
                  </button>
                </div>
              </div>
            ) : (
              <form onSubmit={(e) => handleFormSubmit(e, false)} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Leave Type</label>
                  <select
                    value={leaveType}
                    onChange={(e) => setLeaveType(e.target.value)}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                  >
                    <option value="ANNUAL">Annual Leave</option>
                    <option value="SICK">Sick Leave</option>
                    <option value="EMERGENCY">Emergency Leave</option>
                  </select>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Start Date</label>
                    <input
                      type="date"
                      required
                      value={startDate}
                      onChange={(e) => setStartDate(e.target.value)}
                      className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">End Date</label>
                    <input
                      type="date"
                      required
                      value={endDate}
                      onChange={(e) => setEndDate(e.target.value)}
                      className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                    />
                  </div>
                </div>

                {isRetroactiveDate && (
                  <div className="p-3 rounded-xl bg-amber-50 border border-amber-200 text-amber-800 text-xs flex items-center gap-2">
                    <Clock className="w-4 h-4 text-amber-600 shrink-0" />
                    <span>Start date is in the past (Retroactive Leave). An explicit reason is required.</span>
                  </div>
                )}

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Reason {isRetroactiveDate ? '(Required for Retroactive Leave)' : '(Optional)'}
                  </label>
                  <input
                    type="text"
                    required={isRetroactiveDate}
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    placeholder="e.g. Medical emergency, planned family trip"
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:border-sky-500"
                  />
                </div>

                <div className="flex items-center justify-end gap-3 pt-3">
                  <button
                    type="button"
                    onClick={() => setIsSubmitOpen(false)}
                    className="px-4 py-2 text-sm font-medium text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-xl cursor-pointer"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="px-4 py-2 text-sm font-semibold text-white bg-sky-600 hover:bg-sky-700 rounded-xl shadow-md cursor-pointer"
                  >
                    Submit Request
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}

      {/* Decision Dialog */}
      <ConfirmDialog
        isOpen={!!decisionReq}
        title={`${decisionAction === 'APPROVED' ? 'Approve' : 'Reject'} Leave Request`}
        message={`Are you sure you want to ${decisionAction.toLowerCase()} leave request for ${decisionReq?.employeeName} (${decisionReq?.startDate} to ${decisionReq?.endDate})?`}
        confirmLabel={`${decisionAction === 'APPROVED' ? 'Approve' : 'Reject'}`}
        isDanger={decisionAction === 'REJECTED'}
        onConfirm={handleConfirmDecision}
        onCancel={() => setDecisionReq(null)}
      />
    </div>
  );
};
