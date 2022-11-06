package roach

import scalanative.unsafe.*


opaque type ParamValues = Ptr[CString] 
object ParamValues:
  inline def apply(inline v: Ptr[CString]): ParamValues = v
  
