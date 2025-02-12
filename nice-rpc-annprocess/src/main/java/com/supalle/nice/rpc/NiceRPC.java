package com.supalle.nice.rpc;

import java.lang.annotation.*;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE
        , ElementType.CONSTRUCTOR
        , ElementType.FIELD
        , ElementType.METHOD
        , ElementType.PARAMETER})
@Documented
public @interface NiceRPC {


}
