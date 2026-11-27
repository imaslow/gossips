import { AuthProvider, useAuth } from "@/hooks/use-auth";
import AuthView from "@/sections/AuthView";
import MessengerView from "@/sections/MessengerView";

function Root() {
  const { session } = useAuth();
  return session ? <MessengerView /> : <AuthView />;
}

export default function App() {
  return (
    <AuthProvider>
      <Root />
    </AuthProvider>
  );
}
