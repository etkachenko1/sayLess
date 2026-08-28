import {useState} from "react";
import {CheckCircle, Trash2, CalendarDays, User, Clock, Edit2, Brain} from "lucide-react"
import { API_URL } from "../config/api"
import { getIdFromToken } from "../utils/getIdFromToken"

interface Task {
  id: string;
  title: string;
  description: string;
  status: string;
  createdById: string;
  createdByName: string;
  assignedToId:string;
  assignedToName: string;
  deadline: string;
}

interface TaskListProps {
  tasks: Task[];
  loading: boolean;
  onToggleStatus: (id: string, status: string) =>void;
  onDelete: (id: string) => void;
  onEdit: (task: Task) => void;
}

export default function TaskList({
  tasks,
  loading,
  onToggleStatus,
  onDelete,
  onEdit,
}: TaskListProps) {
  if (loading) return (<div className="text-center text-gray-400 animate-pulse py-6">Loading tasks...</div>);

  const [predictions, setPredictions] = useState<Record<string, number>>({});
  const token = localStorage.getItem("token");
  const myId = getIdFromToken();

  const handlePredict = async (task: Task) => {
    if(!token) return;

    try {
      const res = await fetch(`${API_URL}/ai/predict`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization : `Bearer ${token}`,
        },

        body:JSON.stringify({
          user_id: task.assignedToId || task.createdById,
          text: `${task.title ?? ""} ${task.description ?? ""}`,
          deadline: task.deadline,
          assigned_by: task.createdById,
        }),
      });
      if (!res.ok) throw new Error(`AI service error: ${res.status}`);
      const data = await res.json();
      setPredictions((prev) => ({ ...prev, [task.id]: data.likelihood }));
   } catch (err) {
    console.error("Prediction failed", err); }
    };

  if (tasks.length === 0) return (<div className="text-center text-gray-500 py-6 italic">No tasks, take a break ☕.</div>);
  const getAccentColor = (status: string) => {
    switch (status) {
      case "DONE":
        return "bg-green-500";
      case "IN_PROGRESS":
        return "bg-yellow-500";
      default:
        return "bg-gray-600";
    }
  };

  const isOverdue = (deadline?: string, status?: string) => {
    if (!deadline || status === "DONE") return false;
    return new Date(deadline).getTime() < Date.now();
  };
  const formatDate = (dateString?: string) => {
    if (!dateString) return "—";
    const date = new Date(dateString);
    return date.toLocaleString(undefined, {
      month: "short",
      day: "numeric",
      year: "numeric",
      hour: "numeric",
      minute: "2-digit",
    });
  };

  const sortedTasks = [...tasks].sort((a, b) => {
    const aDone = a.status === "DONE";
    const bDone = b.status === "DONE";
    if (aDone !== bDone) return aDone ? 1 : -1;

    if (!a.deadline && !b.deadline) return 0;
    if (!a.deadline) return 1;
    if (!b.deadline) return -1;
    return new Date(a.deadline).getTime() - new Date(b.deadline).getTime();
  });

return (
    <ul className="space-y-4">
      {sortedTasks.map((t) => {
        const overdue = isOverdue(t.deadline, t.status);

        return (
          <li
            key={t.id}
            className={`group relative border border-gray-700 rounded-xl flex flex-col transition-all duration-200 overflow-hidden ${
              t.status === "DONE"
                ? "bg-gray-700/60 opacity-80"
                : "bg-gray-800/70 hover:bg-gray-700/70"
            }`}
          >
            {/* Accent color bar */}
            <div
              className={`absolute left-0 top-0 h-full w-1.5 ${getAccentColor(
                t.status
              )}`}
            />

            {/* Content */}
            <div className="min-w-0 p-5 pl-6">
              <div className="flex flex-wrap items-center gap-2">
                <p className="text-lg font-semibold text-white break-words min-w-0">{t.title}</p>
                <span
                  className={`px-2 py-0.5 rounded-full text-xs font-semibold ${
                    t.status === "DONE"
                      ? "bg-green-600/30 text-green-300 border border-green-600/40"
                      : "bg-yellow-600/20 text-yellow-400 border border-yellow-600/40"
                  }`}
                >
                  {t.status}
                </span>
                {overdue && (
                  <span className="flex items-center gap-1 text-xs text-red-400 font-semibold">
                    <Clock className="h-3.5 w-3.5" />
                    Overdue
                  </span>
                )}
              </div>

              {t.description && (
                <p className="text-sm text-gray-400 mt-1 break-words">{t.description}</p>
              )}

              <div className="flex flex-wrap items-center gap-x-4 gap-y-1 mt-3 text-sm text-gray-400">
                {t.deadline && (
                  <span
                    className={`flex items-center gap-1 ${
                      overdue ? "text-red-400 font-medium" : "text-gray-400"
                    }`}
                  >
                    <CalendarDays className="h-4 w-4" />
                    {formatDate(t.deadline)}
                  </span>
                )}
                {t.assignedToName && (
                  <span className="flex items-center gap-1">
                    <User className="h-4 w-4 text-gray-400" />
                    Assigned to{" "}
                    <span className="font-semibold text-gray-300"> {t.assignedToName}</span>
                  </span>
                )}

                {t.createdByName && (
                  <span className="flex items-center gap-1">
                    <User className="h-4 w-4 text-gray-400" />
                    Created by{" "}
                    <span className="font-semibold text-gray-300">
                      {t.createdByName}
                    </span>
                  </span>
                )}
              </div>
              {predictions[t.id] !== undefined && (
                <div className="mt-2 text-sm text-purple-300 font-medium">
                  Probability: {(predictions[t.id] * 100).toFixed(1)}%
                </div>
              )}
            </div>

            {/* Buttons */}
            <div className="flex items-center flex-wrap gap-2 px-5 pb-5 pl-6">
                <button
                  onClick={() => onToggleStatus(t.id, t.status)}
                  className={`flex items-center gap-1 text-sm text-white transition-colors px-3 py-1.5 rounded-lg shadow-sm ${
                      t.status === "DONE"
                      ? "bg-yellow-600/90 hover:bg-yellow-500"
                      : "bg-green-600/90 hover:bg-green-500"
                  }`}
                >
                  <CheckCircle className="h-4 w-4" />{" "}
                  {t.status === "DONE" ? "Undo" : "Done"}
                </button>

                {t.createdById === myId && (
                  <button
                    onClick={() => onEdit(t)}
                    className="flex items-center gap-1 text-sm text-white bg-blue-600/90 hover:bg-blue-500 transition-colors px-3 py-1.5 rounded-lg shadow-sm"
                  >
                  <Edit2 className="h-4 w-4"/> Edit
                  </button>
                )}
                <button
                onClick={() => handlePredict(t)}
                className="flex items-center gap-1 text-sm text-white bg-purple-600/90 hover:bg-purple-500 transition-colors px-3 py-1.5 rounded-lg shadow-sm"
                >
                  <Brain className="h-4 w-4" /> Predict </button>
              {t.createdById === myId && (
                <button
                  onClick={() => onDelete(t.id)}
                  className="flex items-center gap-1 text-sm text-white bg-red-700/90 hover:bg-red-600 transition-colors px-3 py-1.5 rounded-lg shadow-sm"
                >
                  <Trash2 className="h-4 w-4" /> Delete
                </button>
              )}
            </div>
          </li>
        );
      })}
    </ul>
  );
}