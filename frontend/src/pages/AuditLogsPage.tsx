import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { Shield, RefreshCw, AlertCircle, ChevronLeft, ChevronRight, Eye, Code } from 'lucide-react';

interface AuditLog {
  id: string;
  entityType: string;
  entityId: string;
  action: string;
  performedByUsername: string;
  performedByRole: string;
  oldValue: string | null;
  newValue: string | null;
  performedAt: string;
}

interface PaginatedAuditResponse {
  content: AuditLog[];
  totalPages: number;
  totalElements: number;
  number: number;
}

export const AuditLogsPage: React.FC = () => {
  const { isAdmin } = useAuth();
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  const [filterEntityType, setFilterEntityType] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Selected Log for detail modal
  const [selectedLog, setSelectedLog] = useState<AuditLog | null>(null);

  useEffect(() => {
    fetchAuditLogs(page, filterEntityType);
  }, [page]);

  const fetchAuditLogs = async (pageNum: number, entityType: string) => {
    setLoading(true);
    setError(null);
    try {
      const token = localStorage.getItem('access_token');
      const query = `?page=${pageNum}&size=15${entityType ? `&entityType=${encodeURIComponent(entityType)}` : ''}`;
      const res = await fetch(`/api/audit-logs${query}`, {
        headers: { Authorization: `Bearer ${token}` },
      });

      if (res.ok) {
        const data: PaginatedAuditResponse = await res.json();
        setLogs(data.content);
        setTotalPages(data.totalPages || 1);
        setTotalElements(data.totalElements || 0);
      } else {
        setError('Failed to fetch audit log entries');
      }
    } catch (e) {
      setError('Network error fetching audit logs');
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    fetchAuditLogs(0, filterEntityType);
  };

  const formatJsonPretty = (jsonStr: string | null) => {
    if (!jsonStr) return <span className="text-slate-400 italic">None</span>;
    try {
      const obj = JSON.parse(jsonStr);
      return (
        <pre className="bg-slate-900 text-emerald-400 p-3 rounded-xl text-[11px] overflow-x-auto font-mono">
          {JSON.stringify(obj, null, 2)}
        </pre>
      );
    } catch (e) {
      return <span className="font-mono text-slate-700">{jsonStr}</span>;
    }
  };

  if (!isAdmin) {
    return (
      <div className="p-8 text-center bg-rose-50 border border-rose-200 rounded-2xl text-rose-800 space-y-2">
        <Shield className="w-10 h-10 text-rose-600 mx-auto" />
        <h3 className="text-base font-bold">Access Restricted</h3>
        <p className="text-xs text-rose-700">Audit log inspection is restricted strictly to System Administrators.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <Shield className="w-6 h-6 text-rose-600" />
            <span>Audit Log Explorer</span>
          </h2>
          <p className="text-xs text-slate-500 font-medium">
            System activity history, entity state mutations, and user action tracking
          </p>
        </div>

        <form onSubmit={handleSearch} className="flex items-center gap-2">
          <input
            type="text"
            placeholder="Filter by Entity Type (e.g. Schedule)..."
            value={filterEntityType}
            onChange={(e) => setFilterEntityType(e.target.value)}
            className="px-3.5 py-2 bg-white border border-slate-300 rounded-xl text-xs font-medium focus:ring-2 focus:ring-rose-500"
          />
          <button
            type="submit"
            className="px-4 py-2 bg-slate-900 text-white font-semibold text-xs rounded-xl hover:bg-slate-800 transition-colors cursor-pointer"
          >
            Filter
          </button>
        </form>
      </div>

      {error && (
        <div className="p-4 bg-rose-50 border border-rose-200 text-rose-800 rounded-xl text-sm flex items-center gap-2">
          <AlertCircle className="w-5 h-5 text-rose-600" />
          <span>{error}</span>
        </div>
      )}

      {loading ? (
        <div className="glass-card p-12 text-center text-slate-500 rounded-2xl flex items-center justify-center gap-2">
          <RefreshCw className="w-5 h-5 animate-spin text-rose-600" />
          <span className="text-sm font-semibold">Loading audit logs...</span>
        </div>
      ) : logs.length === 0 ? (
        <div className="glass-card p-12 text-center text-slate-500 rounded-2xl space-y-3">
          <Shield className="w-12 h-12 text-slate-400 mx-auto" />
          <h3 className="text-base font-bold text-slate-800">No Audit Logs Found</h3>
          <p className="text-xs text-slate-500">No audit events match your filter criteria.</p>
        </div>
      ) : (
        <div className="glass-card p-6 rounded-2xl space-y-4">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="bg-slate-100 text-slate-700 font-bold uppercase tracking-wider border-b border-slate-200">
                  <th className="p-3">Timestamp</th>
                  <th className="p-3">Entity Type</th>
                  <th className="p-3">Entity ID</th>
                  <th className="p-3">Action</th>
                  <th className="p-3">Performed By</th>
                  <th className="p-3 text-right">Details</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200">
                {logs.map((log) => (
                  <tr key={log.id} className="hover:bg-slate-50">
                    <td className="p-3 font-mono text-slate-600">
                      {new Date(log.performedAt).toLocaleString()}
                    </td>
                    <td className="p-3 font-bold text-slate-900">{log.entityType}</td>
                    <td className="p-3 font-mono text-slate-500">{log.entityId.substring(0, 8)}...</td>
                    <td className="p-3">
                      <span
                        className={`badge ${
                          log.action === 'CREATE'
                            ? 'badge-active'
                            : log.action === 'APPROVE'
                            ? 'badge-info'
                            : log.action === 'DELETE'
                            ? 'badge-danger'
                            : 'badge-warning'
                        }`}
                      >
                        {log.action}
                      </span>
                    </td>
                    <td className="p-3 font-semibold text-slate-800">
                      {log.performedByUsername} ({log.performedByRole})
                    </td>
                    <td className="p-3 text-right">
                      <button
                        onClick={() => setSelectedLog(log)}
                        className="px-2.5 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-[11px] rounded-lg transition-colors cursor-pointer flex items-center gap-1 ml-auto"
                      >
                        <Eye className="w-3.5 h-3.5" />
                        <span>Inspect Payload</span>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          <div className="flex justify-between items-center pt-3 border-t border-slate-200 text-xs text-slate-500">
            <span>Total Log Entries: {totalElements}</span>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="px-3 py-1.5 bg-slate-100 hover:bg-slate-200 disabled:opacity-40 rounded-lg font-semibold flex items-center gap-1 cursor-pointer"
              >
                <ChevronLeft className="w-4 h-4" /> Previous
              </button>
              <span className="font-bold text-slate-700">
                Page {page + 1} of {totalPages}
              </span>
              <button
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="px-3 py-1.5 bg-slate-100 hover:bg-slate-200 disabled:opacity-40 rounded-lg font-semibold flex items-center gap-1 cursor-pointer"
              >
                Next <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Inspect Payload Modal */}
      {selectedLog && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-2xl w-full p-6 space-y-4 shadow-2xl">
            <div className="flex justify-between items-center border-b border-slate-200 pb-3">
              <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <Code className="w-5 h-5 text-rose-600" />
                <span>Audit State Payload Inspection</span>
              </h3>
              <button onClick={() => setSelectedLog(null)} className="text-slate-400 hover:text-slate-600 font-bold">
                ✕
              </button>
            </div>

            <div className="grid grid-cols-2 gap-4 text-xs">
              <div>
                <h4 className="font-bold text-slate-700 mb-1">Old State Value:</h4>
                {formatJsonPretty(selectedLog.oldValue)}
              </div>
              <div>
                <h4 className="font-bold text-slate-700 mb-1">New State Value:</h4>
                {formatJsonPretty(selectedLog.newValue)}
              </div>
            </div>

            <div className="flex justify-end pt-2 border-t border-slate-200">
              <button
                onClick={() => setSelectedLog(null)}
                className="px-4 py-2 bg-slate-200 hover:bg-slate-300 text-slate-800 font-semibold text-xs rounded-xl"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
