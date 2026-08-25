export const MIN_PASSWORD_LENGTH = 8;

const COMMON_PASSWORDS = new Set([
  "password1", "password123", "qwerty123", "welcome1", "welcome123", "letmein1",
  "letmein123", "admin1234", "iloveyou1", "monkey123", "football1", "baseball1", "dragon123",
  "master123", "sunshine1", "princess1", "trustno1", "superman1", "changeme1",
  "passw0rd", "abc123456", "whatever1", "starwars1", "shadow123", "michael1",
]);

export interface PasswordRequirement {
  label: string;
  met: boolean;
}

export function getPasswordRequirements(password: string): PasswordRequirement[] {
  return [
    { label: `At least ${MIN_PASSWORD_LENGTH} characters`, met: password.length >= MIN_PASSWORD_LENGTH },
    { label: "One uppercase letter", met: /[A-Z]/.test(password) },
    { label: "One number", met: /[0-9]/.test(password) },
    { label: "Not a commonly used password", met: !COMMON_PASSWORDS.has(password.toLowerCase()) },
  ];
}

export function isPasswordValid(password: string): boolean {
  return getPasswordRequirements(password).every((r) => r.met);
}
