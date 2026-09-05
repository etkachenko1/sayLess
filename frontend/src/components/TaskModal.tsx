import { useEffect } from "react";
import TaskForm from "./TaskForm";

interface TaskModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  description: string;
  assignedTo: string;
  deadline: string;
  onTitleChange: (v: string) => void;
  onDescriptionChange: (v: string) => void;
  onAssignedToChange: (v: string) => void;
  onDeadlineChange: (v: string) => void;
  onSubmit: () => Promise<boolean>;
  isEditing?:boolean;
  friends: {id: string, username: string}[];
}

export default function TaskModal({
  isOpen,
  onClose,
  title,
  description,
  assignedTo,
  deadline,
  onTitleChange,
  onDescriptionChange,
  onAssignedToChange,
  onDeadlineChange,
  onSubmit,
  isEditing = false,
  friends,
}: TaskModalProps) {
  useEffect(() => {
    if (!isOpen) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-40 z-50 overflow-y-auto">
      <div className="min-h-full flex items-start justify-center p-4">
        <div className="bg-gray-800 rounded-xl shadow-lg w-full max-w-lg p-6 my-8">
          <h2 className="text-xl font-bold mb-4 text-center text-red-700">
            {isEditing ? "Edit Task" : "Create New Task" } </h2>
          <TaskForm
            title={title}
            description={description}
            assignedTo={assignedTo}
            deadline={deadline}
            onTitleChange={onTitleChange}
            onDescriptionChange={onDescriptionChange}
            onAssignedToChange={onAssignedToChange}
            onDeadlineChange={onDeadlineChange}
            onSubmit={async () => {
              const success = await onSubmit();
              if (success) onClose();
              return success;
            }}
            isEditing={isEditing}
            friends={friends}
          />
          <div className="text-center">
            <button
              onClick={onClose}
              className="mt-2 text-gray-500 text-sm hover:underline"
            >
              Cancel
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
