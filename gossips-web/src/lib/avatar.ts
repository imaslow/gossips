/**
 * Детерминированный цвет аватара по имени: один и тот же пользователь
 * одинаково выглядит у всех собеседников. Hue из хэша имени, мягкая
 * насыщенность/яркость под тёмную тему.
 */
export function avatarStyle(name: string): { backgroundColor: string; color: string } {
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = (hash * 31 + name.charCodeAt(i)) | 0;
  }
  const hue = ((hash % 360) + 360) % 360;
  return {
    backgroundColor: `hsl(${hue} 45% 22%)`,
    color: `hsl(${hue} 70% 72%)`,
  };
}
