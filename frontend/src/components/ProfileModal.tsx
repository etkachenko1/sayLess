import { useState, useEffect } from "react";

interface ProfileModalProps {
  isOpen: boolean;
  onClose: () => void;
  bio?: string;
  profilePic?: string;
  onSubmit: (bio: string, profilePic: string) => Promise<string | null>;
}

export default function ProfileModal({
  isOpen,
  onClose,
  bio: initialBio,
  profilePic: initialProfilePic,
  onSubmit,
}: ProfileModalProps) {
  const [bio, setBio] = useState(initialBio || "");
  const [profilePic, setProfilePic] = useState(initialProfilePic || "");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setBio(initialBio || "");
      setProfilePic(initialProfilePic || "");
      setError(null);
    }
  }, [isOpen, initialBio, initialProfilePic]);

  if (!isOpen) return null;

  const handleSubmit = async () => {
    setIsSubmitting(true);
    setError(null);
    try {
      const errorMessage = await onSubmit(bio, profilePic);
      if (errorMessage) {
        setError(errorMessage);
      } else {
        onClose();
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-40 flex items-center justify-center z-50">
      <div className="bg-gray-800 rounded-xl shadow-lg w-full max-w-lg p-6">
        <h2 className="text-xl font-bold mb-4 text-center text-red-700">Edit Profile</h2>

        {error && (
          <p className="text-sm text-red-400 bg-red-950 border border-red-800 rounded-lg p-2 mb-4">
            {error}
          </p>
        )}

        <div className="flex flex-col gap-3 mb-6">
          <div className="flex flex-col">
            <label className="text-sm font-medium mb-1 text-gray-300">Bio</label>
            <textarea
              value={bio}
              onChange={(e) => setBio(e.target.value)}
              placeholder="Tell people a bit about yourself"
              maxLength={500}
              rows={3}
              className="border border-gray-600 bg-gray-700 text-white rounded-lg p-3 focus:ring-red-500 resize-none"
            />
            <p className="text-xs text-gray-500 mt-1">{bio.length}/500</p>
          </div>

          <div className="flex flex-col">
            <label className="text-sm font-medium mb-1 text-gray-300">Profile picture URL</label>
            <input
              value={profilePic}
              onChange={(e) => setProfilePic(e.target.value)}
              placeholder="https://..."
              className="border border-gray-600 bg-gray-700 text-white rounded-lg p-3 focus:ring-red-500"
            />
            <p className="text-xs text-gray-500 mt-1">Must be an https link.</p>
          </div>
        </div>

        <button
          disabled={isSubmitting}
          onClick={handleSubmit}
          className="bg-blue-700 hover:bg-blue-600 text-white px-4 py-2 rounded-lg w-full transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isSubmitting ? "Saving..." : "Save changes"}
        </button>

        <div className="text-center">
          <button onClick={onClose} className="mt-2 text-gray-500 text-sm hover:underline">
            Cancel
          </button>
        </div>
      </div>
    </div>
  );
}
