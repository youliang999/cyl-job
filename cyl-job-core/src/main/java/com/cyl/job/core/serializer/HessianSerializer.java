package com.cyl.job.core.serializer;//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//


import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class HessianSerializer {
    public HessianSerializer() {
    }

    public <T> byte[] serialize(T obj) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Hessian2Output ho = new Hessian2Output(os);

        byte[] var5;
        try {
            ho.writeObject(obj);
            ho.flush();
            byte[] result = os.toByteArray();
            var5 = result;
        } catch (IOException var17) {
            throw new RuntimeException(var17);
        } finally {
            try {
                ho.close();
            } catch (IOException var16) {
                throw new RuntimeException(var16);
            }

            try {
                os.close();
            } catch (IOException var15) {
                throw new RuntimeException(var15);
            }
        }

        return var5;
    }

    public <T> Object deserialize(byte[] bytes, Class<T> clazz) {
        ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        Hessian2Input hi = new Hessian2Input(is);

        Object var6;
        try {
            Object result = hi.readObject();
            var6 = result;
        } catch (IOException var18) {
            throw new RuntimeException(var18);
        } finally {
            try {
                hi.close();
            } catch (Exception var17) {
                throw new RuntimeException(var17);
            }

            try {
                is.close();
            } catch (IOException var16) {
                throw new RuntimeException(var16);
            }
        }

        return var6;
    }
}
