import { useState } from "react";
import { Datepicker } from "flowbite-react";

interface TaskFormProps {
  title: string
  description: string
  assignedTo: string
  deadline: string
  onTitleChange: (v: string) => void
  onDescriptionChange: (v: string) => void
  onAssignedToChange: (v: string) => void
  onDeadlineChange: (v: string) => void
  onSubmit: () => Promise<boolean>
  isEditing?: boolean
  friends: {id: string; username:string}[];
}

export default function TaskForm({
  title,
  description,
  assignedTo,
  deadline,
  onTitleChange,
  onDescriptionChange,
  onAssignedToChange,
  onDeadlineChange,
  onSubmit,
  isEditing,
  friends
}: TaskFormProps) {
  const [isSubmitting, setIsSubmitting] = useState(false);

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mb-6">
      <div className="flex flex-col">
        <label className="text-sm font-medium mb-1 text-gray-300">Title</label>
      <input
        value={title}
        onChange={(e) => onTitleChange(e.target.value)}
        placeholder="Task title"
        className="border border-gray-600 bg-gray-700 text-white rounded-lg p-3 focus: ring-red-500"
      />
      </div>
      <div className="flex flex-col">
      <label className="text-sm font-medium mb-1 text-gray-300">Assign to (defaults to me)</label>
      <select
        value={assignedTo}
        onChange={(e) => onAssignedToChange(e.target.value)}
        className="border border-gray-600 bg-gray-700 text-grey-400 rounded-lg p-3"
      >
        {friends.map(f => (
          <option key={f.id} value={f.id}>{f.username}</option>
          ))}
      </select>
      </div>
      <div className="flex flex-col col-span-2">
        <label className="text-sm font-medium mb-1 text-gray-300">Description</label>
        <input
        value={description}
        onChange={(e) => onDescriptionChange(e.target.value)}
        placeholder="Description"
        className="border border-gray-600 bg-gray-700 text-white rounded-lg p-3"
      />
      </div>
      <div className="flex flex-col col-span-2">
        <label className="text-sm font-medium mb-1 text-gray-300">Deadline</label>
        <div className="flex flex-col sm:flex-row items-stretch gap-3">
          <div className="flex-1 relative">
            <Datepicker
              value={deadline ? new Date(deadline) : null}
              onChange={(date: Date | null) => {
                if (date) {
                  const current = deadline ? new Date(deadline) : null;
                  if(current) {
                    date.setHours(current.getHours(), current.getMinutes(), 0, 0);
                  } else {
                    date.setHours(23,59,0,0);
                  }
                  onDeadlineChange(date.toISOString());
                }
              }}
              language="en"
              labelTodayButton="Today"
              labelClearButton="Clear"
              className="[&_input]:bg-gray-700 [&_input]:text-white [&_input]:border-gray-600 [&_input]:rounded-lg [&_input]:h-[48px] [&_input]:focus:ring-red-500 [&_input]:focus:border-red-500 [&_button]:text-gray-300"

            />
          </div>
          <input
            type="time"
            value={deadline ? new Date(deadline).toTimeString().slice(0, 5) : "23:59"}
            onChange={(e) => {
              const [hours, minutes] = e.target.value.split(":");
              const newDate = deadline ? new Date(deadline) : new Date();
              newDate.setHours(parseInt(hours), parseInt(minutes), 0, 0);
              onDeadlineChange(newDate.toISOString());
            }}
            className="border border-gray-600 bg-gray-700 text-white rounded-lg p-3 h-[48px] focus:ring-2 focus:ring-red-400 w-40"
          />
        </div>

        {deadline && (
          <p className="text-xs text-gray-500 mt-2">
            Selected:{" "}
            {new Date(deadline).toLocaleString(undefined, {
              dateStyle: "medium",
              timeStyle: "short",
            })}
          </p>
        )}
      </div>

      <button
        disabled={isSubmitting}
        onClick={async () => {
          setIsSubmitting(true);
          try {
            await onSubmit();
          } finally {
            setIsSubmitting(false);
          }
        }}
        className={`${
          isEditing ? "bg-blue-700 hover:bg-blue-600" : "bg-red-700 hover:bg-red-600"
          } text-white px-4 py-2 rounded-lg col-span-2 transition-colors disabled:opacity-50 disabled:cursor-not-allowed`}
          > {isSubmitting ? "Saving..." : isEditing ? "Update Task" : "Create Task"}
        </button>
    </div>
  )
}
