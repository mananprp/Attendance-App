import {
  Injectable,
  CanActivate,
  ExecutionContext,
  ForbiddenException,
} from '@nestjs/common';

@Injectable()
export class AdminGuard implements CanActivate {
  canActivate(context: ExecutionContext): boolean {
    const request = context.switchToHttp().getRequest();
    const user = request.user;

    if (!user || (user.role !== 'ADMIN' && user.role !== 'SUPERVISOR')) {
      throw new ForbiddenException({
        code: 'FORBIDDEN',
        message: 'Admin or Supervisor access required for this operation.',
      });
    }

    return true;
  }
}
