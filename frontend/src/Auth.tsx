import { useState } from "react"
import type { ChangeEvent } from "react"
import logo from "./assets/sayless-logo.png"
import { API_URL } from "./config/api"
import { getPasswordRequirements, isCommonPassword, isPasswordValid } from "./utils/passwordRules"
import { isValidEmail } from "./utils/emailRules"



interface RegisterData {
    username: string
    email: string
    password: string
}
interface LoginData {
    username: string
    password: string
}

function Auth() {
    const [mode, setMode] = useState<"register" | "login">("login")
    const [registerForm, setRegisterForm] = useState<RegisterData>({
        username:"",
        email:"",
        password: "",
    })
   const [loginForm, setLoginForm] = useState<LoginData>({
        username:"",
        password: "",
    })

    const [error, setError] = useState<string>("")
    const [isSubmitting, setIsSubmitting] = useState(false)

    const switchMode = (next: "register" | "login") => {
        setMode(next)
        setError("")
        setSuccessMessage("")
    }

    const handleChangeRegister = (e: ChangeEvent<HTMLInputElement>) =>{
        const {name, value} = e.target //pulls the name and value of the input
        setRegisterForm((prev) => ({...prev,[name]:value})) // copies previous state, replaces the fiels matvhing the input's name
    }
    const handleChangeLogin = (e: ChangeEvent<HTMLInputElement>) =>{
        const {name, value} = e.target //pulls the name and value of the input
        setLoginForm((prev) => ({...prev,[name]:value})) // copies previous state, replaces the fiels matvhing the input's name
    }

    const [successMessage, setSuccessMessage] = useState<string>("");
    const register = async () => {
        if (isSubmitting) return
        setError("")
        setSuccessMessage("");

        if (!registerForm.username.trim()) { setError("Username is required"); return; }
        if (!registerForm.email.trim()) { setError("Email is required"); return; }
        if (!isValidEmail(registerForm.email)) { setError("Please enter a valid email address"); return; }
        if (isCommonPassword(registerForm.password)) { setError("This password is too common. Please choose a different one"); return; }
        if (!isPasswordValid(registerForm.password)) { setError("Please meet all the password requirements below"); return; }

        setIsSubmitting(true)
        try {
            //send HTTP Post requesr to backend
            const res = await fetch(`${API_URL}/auth/register`, {
                method: "POST",
                headers: {"Content-Type" : "application/json"},
                body: JSON.stringify(registerForm),
            }) 
            const data = await res.json().catch(() => ({})) //parses the response body from the backend into JS object

            if(!res.ok) {
              if(data.error === "This username is already taken.") {
                setError("That username is already taken. Try another one.")
              }
              else if( data.error === "This email is already taken.") {
                setError("That email is already registered. Try logging instead.")
              } else {
                setError(data.error || "Registration Failed")

              }
                return
            }

            setSuccessMessage("Succesfully registered! You can now sign in.")
            setTimeout(() => {
              setMode("login");
              setSuccessMessage("");
            }, 2000);
        }
        catch(err) {
            console.error("Register error:", err)
            setError("Something went wrong. Try again later.")
        }
        finally {
            setIsSubmitting(false)
        }
    }

    const login = async () => {
        if (isSubmitting) return
        setError("")
        setIsSubmitting(true)
        try {
            const res = await fetch(`${API_URL}/auth/login`, {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(loginForm),
            })
            const data = await res.json().catch(() => ({}))

            if(!res.ok) {
                setError(data.error || "Login failed")
                return
            }
            //check if backend returned a JWT token
            if(data.token) {
                localStorage.setItem("token", data.token)
                window.location.href = "/dashboard"
            } else {
                setError("Invalid username or password")
            }

        }catch (err) {
            console.error("Login error:", err)
            setError("Something went wrong. Please try again later")
        }
        finally {
            setIsSubmitting(false)
        }
    }

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-900">
      <div className="w-full max-w-md bg-gray-800 rounded-2xl shadow-md p-8 border-gray-700">
        {/* Logo */}
        <div className="flex justify-center mb-6">
          <img src={logo} alt="SayLess logo" className="h-20" />
        </div>

        {/* Toggle buttons */}
        <div className="flex justify-center mb-6">
          <button
            onClick={() => switchMode("login")}
            className={`px-4 py-2 rounded-l-lg font-semibold transition ${
              mode === "login"
                ? "bg-red-700 text-white"
                : "bg-gray-700 text-gray-300 hover:bg-gray-600"
            }`}
          >
            Sign In
          </button>
          <button
            onClick={() => switchMode("register")}
            className={`px-4 py-2 rounded-r-lg font-semibold transition ${
              mode === "register"
                ? "bg-red-700 text-white"
                : "bg-gray-700 text-gray-300 hover:bg-gray-600"
            }`}
          >
            Sign Up
          </button>
        </div>

        {/* Error message */}
        {error && (
          <div className="mb-4 p-2 text-sm text-red-400 bg-red-900/30 border border-red-700 rounded">
            {error}
          </div>
        )}
        {successMessage && (
          <div className="mb-4 p-2 text-sm text-green-400 bg-green-900/30 border border-green-700 rounded text-center">
            {successMessage}
          </div>
        )}        

        {/* Login Form */}
        {mode === "login" && (
          <>
            <input
              name="username"
              placeholder="Username"
              value={loginForm.username}
              onChange={handleChangeLogin}
              className="w-full p-3 mb-3 border border-gray-600 bg-gray-700 rounded-lg focus:ring-2 focus:ring-red-700 text-white placeholder-gray-400"
            />
            <input
              name="password"
              type="password"
              placeholder="Password"
              value={loginForm.password}
              onChange={handleChangeLogin}
              className="w-full p-3 mb-4 border border-gray-600 bg-gray-700 rounded-lg focus:ring-2 focus:ring-red-700 text-white placeholder-gray-400"
            />
            <button
              onClick={login}
              disabled={isSubmitting}
              className="w-full bg-red-700 hover:bg-red-600 text-white py-2 rounded-lg font-semibold transition disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isSubmitting ? "Signing In..." : "Sign In"}
            </button>
          </>
        )}

        {/* Register Form */}
        {mode === "register" && (
          <>
            <input
              name="username"
              placeholder="Username"
              value={registerForm.username}
              onChange={handleChangeRegister}
              className="w-full p-3 mb-3 border border-gray-600 bg-gray-700 rounded-lg focus:ring-2 focus:ring-red-700 text-white placeholder-gray-400"
            />

            <input
              name="email"
              placeholder="Email"
              value={registerForm.email}
              onChange={handleChangeRegister}
              className="w-full p-3 mb-3 border border-gray-600 bg-gray-700 rounded-lg focus:ring-2 focus:ring-red-700 text-white placeholder-gray-400"
            />
            {registerForm.email.length > 0 && !isValidEmail(registerForm.email) && (
              <p className="text-xs text-red-400 mt-1 mb-2 leading-tight">Please enter a valid email address</p>
            )}
            <input
              name="password"
              type="password"
              placeholder="Password"
              value={registerForm.password}
              onChange={handleChangeRegister}
              className="w-full p-3 mb-2 border border-gray-600 bg-gray-700 rounded-lg focus:ring-2 focus:ring-red-700 text-white placeholder-gray-400"
            />
            <ul className="text-xs mb-4 space-y-1">
              {getPasswordRequirements(registerForm.password).map((req) => (
                <li key={req.label} className={req.met ? "text-green-400" : "text-gray-400"}>
                  {req.met ? "✓" : "○"} {req.label}
                </li>
              ))}
            </ul>
            <button
              onClick={register}
              disabled={isSubmitting}
              className="w-full bg-red-700 hover:bg-red-600 text-white py-2 rounded-lg font-semibold transition disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isSubmitting ? "Signing Up..." : "Sign Up"}
            </button>
          </>
        )}
      </div>
    </div>
  )
}

export default Auth