import { useState } from "react";
import type { FormEvent } from "react";
import { MessageSquareLock } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ApiError } from "@/lib/api";
import { useAuth } from "@/hooks/use-auth";

/** Экран входа/регистрации. Ограничения полей совпадают со спекой security-api.yml. */
export default function AuthView() {
  const { login, register } = useAuth();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  const submit = (action: "login" | "register") => async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setPending(true);
    try {
      if (action === "login") {
        await login({ username, password });
      } else {
        await register({ username, password });
      }
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Сеть недоступна, попробуйте снова");
    } finally {
      setPending(false);
    }
  };

  return (
    <div className="flex min-h-dvh items-center justify-center bg-background p-4">
      <Card className="w-full max-w-sm">
        <CardHeader className="items-center text-center">
          <div className="mb-2 flex size-12 items-center justify-center rounded-2xl bg-primary/15 text-primary">
            <MessageSquareLock className="size-6" aria-hidden />
          </div>
          <CardTitle className="text-2xl">Gossips</CardTitle>
          <CardDescription>Мессенджер с end-to-end шифрованием</CardDescription>
        </CardHeader>
        <CardContent>
          <Tabs defaultValue="login">
            <TabsList className="grid w-full grid-cols-2">
              <TabsTrigger value="login">Вход</TabsTrigger>
              <TabsTrigger value="register">Регистрация</TabsTrigger>
            </TabsList>

            {(["login", "register"] as const).map((tab) => (
              <TabsContent key={tab} value={tab}>
                <form onSubmit={submit(tab)} className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor={`${tab}-username`}>Имя пользователя</Label>
                    <Input
                      id={`${tab}-username`}
                      value={username}
                      onChange={(e) => setUsername(e.target.value)}
                      minLength={3}
                      maxLength={64}
                      autoComplete="username"
                      required
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor={`${tab}-password`}>Пароль</Label>
                    <Input
                      id={`${tab}-password`}
                      type="password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      minLength={8}
                      maxLength={100}
                      autoComplete={tab === "login" ? "current-password" : "new-password"}
                      required
                    />
                  </div>

                  {error && (
                    <p role="alert" className="text-sm text-destructive">
                      {error}
                    </p>
                  )}

                  <Button type="submit" className="w-full" disabled={pending}>
                    {pending
                      ? "Подождите…"
                      : tab === "login"
                        ? "Войти"
                        : "Создать аккаунт"}
                  </Button>
                </form>
              </TabsContent>
            ))}
          </Tabs>
        </CardContent>
      </Card>
    </div>
  );
}
