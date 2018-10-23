// https://searchcode.com/api/result/92687927/

package net.minecraft.entity.projectile;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;

public abstract class EntityFireball extends Entity {

    private int e = -1;
    private int f = -1;
    private int g = -1;
    private Block h;
    public boolean i; // CanaryMod: private => public; inGround
    public EntityLivingBase a;
    public int j; // CanaryMod: private => public; ticksAlive
    public int au; // CanaryMod: private => public; ticksInAir
    public double b;
    public double c;
    public double d;
    private float motionFactor = 0.95F; // CanaryMod: changeable motion factor

    public EntityFireball(World world) {
        super(world);
        this.a(1.0F, 1.0F);
    }

    protected void c() {
    }

    public EntityFireball(World world, double d0, double d1, double d2, double d3, double d4, double d5) {
        super(world);
        this.a(1.0F, 1.0F);
        this.b(d0, d1, d2, this.z, this.A);
        this.b(d0, d1, d2);
        double d6 = (double) MathHelper.a(d3 * d3 + d4 * d4 + d5 * d5);

        this.b = d3 / d6 * 0.1D;
        this.c = d4 / d6 * 0.1D;
        this.d = d5 / d6 * 0.1D;
    }

    public EntityFireball(World world, EntityLivingBase entitylivingbase, double d0, double d1, double d2) {
        super(world);
        this.a = entitylivingbase;
        this.a(1.0F, 1.0F);
        this.b(entitylivingbase.t, entitylivingbase.u, entitylivingbase.v, entitylivingbase.z, entitylivingbase.A);
        this.b(this.t, this.u, this.v);
        this.M = 0.0F;
        this.w = this.x = this.y = 0.0D;
        d0 += this.aa.nextGaussian() * 0.4D;
        d1 += this.aa.nextGaussian() * 0.4D;
        d2 += this.aa.nextGaussian() * 0.4D;
        double d3 = (double) MathHelper.a(d0 * d0 + d1 * d1 + d2 * d2);

        this.b = d0 / d3 * 0.1D;
        this.c = d1 / d3 * 0.1D;
        this.d = d2 / d3 * 0.1D;
    }

    public void h() {
        if (!this.p.E && (this.a != null && this.a.L || !this.p.d((int) this.t, (int) this.u, (int) this.v))) {
            this.B();
        }
        else {
            super.h();
            this.e(1);
            if (this.i) {
                if (this.p.a(this.e, this.f, this.g) == this.h) {
                    ++this.j;
                    if (this.j == 600) {
                        this.B();
                    }

                    return;
                }

                this.i = false;
                this.w *= (double) (this.aa.nextFloat() * 0.2F);
                this.x *= (double) (this.aa.nextFloat() * 0.2F);
                this.y *= (double) (this.aa.nextFloat() * 0.2F);
                this.j = 0;
                this.au = 0;
            }
            else {
                ++this.au;
            }

            Vec3 vec3 = this.p.U().a(this.t, this.u, this.v);
            Vec3 vec31 = this.p.U().a(this.t + this.w, this.u + this.x, this.v + this.y);
            MovingObjectPosition movingobjectposition = this.p.a(vec3, vec31);

            vec3 = this.p.U().a(this.t, this.u, this.v);
            vec31 = this.p.U().a(this.t + this.w, this.u + this.x, this.v + this.y);
            if (movingobjectposition != null) {
                vec31 = this.p.U().a(movingobjectposition.f.c, movingobjectposition.f.d, movingobjectposition.f.e);
            }

            Entity entity = null;
            List list = this.p.b((Entity) this, this.D.a(this.w, this.x, this.y).b(1.0D, 1.0D, 1.0D));
            double d0 = 0.0D;

            for (int i0 = 0; i0 < list.size(); ++i0) {
                Entity entity1 = (Entity) list.get(i0);

                if (entity1.R() && (!entity1.h(this.a) || this.au >= 25)) {
                    float f0 = 0.3F;
                    AxisAlignedBB axisalignedbb = entity1.D.b((double) f0, (double) f0, (double) f0);
                    MovingObjectPosition movingobjectposition1 = axisalignedbb.a(vec3, vec31);

                    if (movingobjectposition1 != null) {
                        double d1 = vec3.d(movingobjectposition1.f);

                        if (d1 < d0 || d0 == 0.0D) {
                            entity = entity1;
                            d0 = d1;
                        }
                    }
                }
            }

            if (entity != null) {
                movingobjectposition = new MovingObjectPosition(entity);
            }

            if (movingobjectposition != null) {
                this.a(movingobjectposition);
            }

            this.t += this.w;
            this.u += this.x;
            this.v += this.y;
            float f1 = MathHelper.a(this.w * this.w + this.y * this.y);

            this.z = (float) (Math.atan2(this.y, this.w) * 180.0D / 3.1415927410125732D) + 90.0F;

            for (this.A = (float) (Math.atan2((double) f1, this.x) * 180.0D / 3.1415927410125732D) - 90.0F; this.A - this.C < -180.0F; this.C -= 360.0F) {
                ;
            }

            while (this.A - this.C >= 180.0F) {
                this.C += 360.0F;
            }

            while (this.z - this.B < -180.0F) {
                this.B -= 360.0F;
            }

            while (this.z - this.B >= 180.0F) {
                this.B += 360.0F;
            }

            this.A = this.C + (this.A - this.C) * 0.2F;
            this.z = this.B + (this.z - this.B) * 0.2F;
            float f2 = this.e();

            if (this.M()) {
                for (int i1 = 0; i1 < 4; ++i1) {
                    float f3 = 0.25F;

                    this.p.a("bubble", this.t - this.w * (double) f3, this.u - this.x * (double) f3, this.v - this.y * (double) f3, this.w, this.x, this.y);
                }

                // f2 = 0.8F;
                f2 -= 0.15F; // CanaryMod: Change to reduce water speed rather than set it
            }

            this.w += this.b;
            this.x += this.c;
            this.y += this.d;
            this.w *= (double) f2;
            this.x *= (double) f2;
            this.y *= (double) f2;
            this.p.a("smoke", this.t, this.u + 0.5D, this.v, 0.0D, 0.0D, 0.0D);
            this.b(this.t, this.u, this.v);
        }
    }

    public float e() { // CanaryMod: protected => public
        return motionFactor; // CanaryMod: return custom factor
    }

    protected abstract void a(MovingObjectPosition movingobjectposition);

    public void b(NBTTagCompound nbttagcompound) {
        nbttagcompound.a("xTile", (short) this.e);
        nbttagcompound.a("yTile", (short) this.f);
        nbttagcompound.a("zTile", (short) this.g);
        nbttagcompound.a("inTile", (byte) Block.b(this.h));
        nbttagcompound.a("inGround", (byte) (this.i ? 1 : 0));
        nbttagcompound.a("direction", (NBTBase) this.a(new double[]{ this.w, this.x, this.y }));
        nbttagcompound.a("motionFactor", this.motionFactor); // CanaryMod: store motionFactor
    }

    public void a(NBTTagCompound nbttagcompound) {
        this.e = nbttagcompound.e("xTile");
        this.f = nbttagcompound.e("yTile");
        this.g = nbttagcompound.e("zTile");
        this.h = Block.e(nbttagcompound.d("inTile") & 255);
        this.i = nbttagcompound.d("inGround") == 1;
        if (nbttagcompound.c("motionFactor")) { // CanaryMod: If motionFactor is stored, retrive it
            this.motionFactor = nbttagcompound.h("motionFactor");
        }

        if (nbttagcompound.b("direction", 9)) {
            NBTTagList nbttaglist = nbttagcompound.c("direction", 6);

            this.w = nbttaglist.d(0);
            this.x = nbttaglist.d(1);
            this.y = nbttaglist.d(2);
        }
        else {
            this.B();
        }
    }

    public boolean R() {
        return true;
    }

    public float af() {
        return 1.0F;
    }

    public boolean a(DamageSource damagesource, float f0) {
        if (this.aw()) {
            return false;
        }
        else {
            this.Q();
            if (damagesource.j() != null) {
                Vec3 vec3 = damagesource.j().ag();

                if (vec3 != null) {
                    this.w = vec3.c;
                    this.x = vec3.d;
                    this.y = vec3.e;
                    this.b = this.w * 0.1D;
                    this.c = this.x * 0.1D;
                    this.d = this.y * 0.1D;
                }

                if (damagesource.j() instanceof EntityLivingBase) {
                    this.a = (EntityLivingBase) damagesource.j();
                }

                return true;
            }
            else {
                return false;
            }
        }
    }

    public float d(float f0) {
        return 1.0F;
    }

    // CanaryMod
    public void setMotionFactor(float factor) {
        this.motionFactor = factor;
    }
}

