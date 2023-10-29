package mr1

import spinal.core._

case class MR1Config (
    supportMul      : Boolean = true,
    supportDiv      : Boolean = true,
    supportCsr      : Boolean = true,
    supportFormal   : Boolean = true,
    supportFence    : Boolean = true,
                     ) {
    def hasMul = supportMul
    def hasDiv = supportMul
    def hasCsr = supportCsr
    def hasFence = supportFence
    def hasFormal = supportFormal
}

case class RVFI(config: MR1Config) extends Bundle {
    val valid     = Bool()
    val order     = UInt(64 bits)
    val insn      = Bits (32 bits)
    val trap      = Bool 
    val halt      = Bool() 
    val intr      = Bool 
    val rs1_addr = Bits (5 bits)
    val rs2_addr = Bits (5 bits)
    val rs1_rdata = Bits(32 bits)
    val rs2_rdata = Bits(32 bits)
    val rd_addr   = Bits(5 bits)
    val rd_wdata  = Bits(32 bits)
    val pc_rdata = Bits(32 bits)
    val pc_wdata = Bits(32 bits)
    val mem_addr  = Bits(32 bits)
    val mem_rmask = Bits(4 bits)
    val mem_wmask = Bits(4 bits)
    val mem_rdata = Bits(32 bits)
    val mem_wdata = Bits(32 bits)

    def init() : RVFI = {
        valid init(False)
        this
    }
}


object InstrFormat extends SpinalEnum {
    val Undef = newElement()
    val R = newElement()
    val I = newElement()
    val S = newElement()
    val B = newElement()
    val U = newElement()
    val J = newElement()
}

object InstrType extends SpinalEnum {
    val LUI     = newElement()
    val AUIPC   = newElement()
    val JAL     = newElement()
    val JALR    = newElement()
    val B       = newElement()
    val L       = newElement() 
    val S       = newElement() 
    val ALU_I   = newElement() 
    val SHIFT_I = newElement() 
    val ALU     = newElement() 
    val SHIFT   = newElement() 
    val FENCE   = newElement() 
    val E       = newElement() 
    val CSR     = newElement() 
    val MUL     = newElement() 
    val DIV     = newElement() 
}

class mr1(config: MR1Config) extends Component {
    val io = new Bundle {
        val instr_valid = in Bool()
        val instr = in Bits(32 bits)
        
        val rvfi = if (config.hasFormal) out(RVFI(config).setName("rvfi")) else null
    }
    
    val fetch = new Fetch (config)
    fetch.io.instr_valid := io.instr_valid
    fetch.io.instr := io.instr 
    
    val decode = new Decoder(config)

    fetch.io.f2d <> decode.io.f2d
    fetch.io.d2f <> decode.io.d2f 

    io.rvfi <> decode.io.d2e.rvfi
    
    decode.io.e2d.stall := False
}

object mr1Verilog {
    def main(args: Array[String]) {
        SpinalVerilog(new mr1(config = MR1Config()))
    }
}