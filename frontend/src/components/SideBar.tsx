import defaultPfp from "../assets/defaultPFP.png"
interface SidebarProps {
  username: string;
  bio?: string;
  profilePic? : string;
  onEditProfile: () => void
}

export default function Sidebar({ username, bio, profilePic, onEditProfile }: SidebarProps) {
  return (
    <aside className="bg-gray-800 shadow-lg rounded-xl p-6 w-full md:w-64 flex flex-col items-center">
        <img
          src={profilePic || defaultPfp}
          alt="profile"
          className="w-24 h-24 rounded-full mb-3 border-gray-600"
        />
        <h2 className="text-lg font-semibold">{username}</h2>
        {bio && <p className="text-sm text-gray-400 text-center mt-1 break-words w-full">{bio}</p>}
        <button onClick={onEditProfile} className="mt-4 text-sm bg-gray-700 text-gray-200 px-3 py-1 rounded-lg hover:bg-gray-600"> Edit Profile</button>
    </aside>
  );
}
